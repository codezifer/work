package de.carsten.android.muzzic.scanning

import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.core.net.toUri
import de.carsten.android.muzzic.AppConfig
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.TOP_100
import de.carsten.android.muzzic.UNKNOWN
import de.carsten.android.muzzic.UNKNOWN_ALBUM
import de.carsten.android.muzzic.UNKNOWN_ARTIST
import de.carsten.android.muzzic.UNKNOWN_GENRE
import de.carsten.android.muzzic.id3.Id3
import de.carsten.android.muzzic.logging.logger
import de.carsten.android.muzzic.model.AlbumArtUri
import de.carsten.android.muzzic.persistence.dao.GenreDao
import de.carsten.android.muzzic.persistence.dao.PlaylistDao
import de.carsten.android.muzzic.persistence.dao.SongDao
import de.carsten.android.muzzic.persistence.entity.Playlist
import de.carsten.android.muzzic.persistence.entity.PlaylistSong
import de.carsten.android.muzzic.persistence.entity.Song
import de.carsten.android.muzzic.persistence.repo.AppSettingsRepository
import de.carsten.android.muzzic.utils.GenreUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Implementation of [FileScanner] that scans for music files and extracts their metadata.
 *
 * Files are discovered chunk-wise via [FileScanner.scanForFiles] (Storage Access Framework)
 * and parsed from their content [android.net.Uri] without requiring direct file access.
 * [Song.filePath] stores the content URI string.
 */
class MusicFileScanner(
    private val context: Context,
    private val songDao: SongDao,
    private val genreDao: GenreDao,
    private val playlistDao: PlaylistDao,
    private val appSettingsRepository: AppSettingsRepository,
) : FileScanner {

    companion object {
        const val SCANNER_ID = "MusicFileScanner"
        private val logger = logger()
    }

    override val scannerId: String = SCANNER_ID
    override val notificationTitleRes: Int = R.string.scan_notification_title

    override suspend fun scan(onProgress: ((String, Int) -> Unit)?) = coroutineScope {
        try {
            logger.info("Start to scan for music files...")
            onProgress?.invoke(context.getString(R.string.scan_status_scanning), 0)

            val configuredDir = appSettingsRepository.getMusicDirectory()?.toUri()
            val currentSongs = songDao.getAllSongs().first()
            val supportedFiles = AppConfig.Scanning.SUPPORTED_MUSIC_FILES
            val currentFilePaths = currentSongs.mapNotNull { it.filePath }.toSet()

            val existingFilePaths = mutableSetOf<String>()
            var processedFiles = 0

            // Streaming pass: collect paths that still exist on disk (needed for cleanup) and
            // parse new files chunk-wise while the directory walk continues, so the
            // library is populated incrementally instead of all at once. Single pass avoids duplicate folder traversal.
            scanForFiles(context, configuredDir, supportedFiles).collect { scannedFiles ->
                ensureActive()
                scannedFiles.forEach { existingFilePaths.add(it.uri.toString()) }
                val newFiles = scannedFiles.filter { it.uri.toString() !in currentFilePaths }.toSet()
                if (newFiles.isNotEmpty()) {
                    processChunk(newFiles)
                    processedFiles += newFiles.size
                    reportProgress(processedFiles, onProgress)
                }
            }

            if (processedFiles > 0) {
                updateAutomaticPlaylists()
            }

            onProgress?.invoke(context.getString(R.string.scan_status_cleaning), AppConfig.Scanning.MAX_PROGRESS)
            ensureActive()

            // Clean up songs that no longer exist on disk, BUT keep mock songs and remote URLs
            val missingSongs =
                currentSongs.filter { song ->
                    val path = song.filePath
                    path != null &&
                        !path.startsWith("content://mock") &&
                        !path.startsWith("http://") &&
                        !path.startsWith("https://") &&
                        path !in existingFilePaths
                }

            if (missingSongs.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    songDao.deleteSongs(missingSongs)
                }
            }
        } finally {
            logger.info("... Finished to scan for music files.")
        }
    }

    private suspend fun processChunk(chunk: Set<ScannedFile>) = coroutineScope {
        val songs: List<Song> = chunk.map { file ->
            async(Dispatchers.IO) {
                extractSongMetadata(file)
            }
        }.awaitAll()

        withContext(Dispatchers.IO) {
            songDao.insertSongs(songs)
        }
    }

    private fun reportProgress(processedFiles: Int, onProgress: ((String, Int) -> Unit)?) {
        // Without an expensive count pass, report progress as an indeterminate animation or ongoing count
        onProgress?.invoke(
            context.getString(R.string.scan_status_metadata, processedFiles),
            -1, // -1 or custom value to signal an indeterminate progress state
        )
    }

    /**
     * Extracts song metadata for a scanned file via its content URI stream.
     *
     * Falls back to an UNKNOWN placeholder song if the stream cannot be opened
     * or the tag cannot be parsed.
     *
     * @param file scanned file.
     * @return extracted or fallback [Song] with the content URI string as file path.
     */
    private fun extractSongMetadata(file: ScannedFile): Song {
        val filePath = file.uri.toString()
        return try {
            val metadata = context.contentResolver.openInputStream(file.uri)?.use { input ->
                Id3.parse(input, logger, file.displayName)
            } ?: return fallbackSong(filePath)
            val duration = metadata.duration ?: readDuration(file)
            val albumArt = metadata.albumArt?.takeIf { it.isValid() }?.let { art ->
                AlbumArtUri(
                    filePath = filePath,
                    offset = art.offset.toLong(),
                    size = art.length.toLong(),
                    hashCode = art.hashCode ?: 0,
                    mimeType = art.mimeType,
                ).get()
            }
            Song.fromId3(filePath, metadata, albumArt, duration)
        } catch (e: Exception) {
            logger.error("Failed to extract metadata for $filePath", e)
            fallbackSong(filePath)
        }
    }

    /**
     * Reads the playable duration for a scanned file.
     *
     * Used when the ID3 tag carries no TLEN frame.
     *
     * @param file scanned file.
     * @return duration in milliseconds, or 0 if it cannot be determined.
     */
    private fun readDuration(file: ScannedFile): Long = try {
        MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(context, file.uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        }
    } catch (e: Exception) {
        logger.error("Failed to read duration for ${file.uri}", e)
        0L
    }

    /**
     * Creates a placeholder song for files without readable metadata.
     *
     * @param filePath content URI string stored as song file path.
     * @return UNKNOWN [Song].
     */
    private fun fallbackSong(filePath: String): Song = Song(
        title = UNKNOWN,
        artist = UNKNOWN_ARTIST,
        album = UNKNOWN_ALBUM,
        genre = UNKNOWN_GENRE,
        duration = 0L,
        filePath = filePath,
    )

    /**
     * Refreshes the auto-generated Top-100 playlists per genre.
     *
     * Spelling variants sharing a normalized key (see [GenreUtils.normalizeKey],
     * e.g. "Death Core", "Death-Core", "Deathcore") are merged into a single
     * playlist named after the canonical variant (see [GenreUtils.getCanonicalName]).
     * Stale auto-playlists whose names no longer match a canonical group are removed.
     */
    suspend fun updateAutomaticPlaylists() = withContext(Dispatchers.IO) {
        val genreCounts = genreDao.getGenreCounts().filter { it.genre.isNotBlank() }.associate { it.genre to it.count }
        val groups = GenreUtils.groupByNormalizedKey(genreCounts)
        val canonicalNames = groups.values.map { GenreUtils.getCanonicalName(it) }.toSet()
        val canonicalPlaylistNames = canonicalNames.map { "$it - $TOP_100" }.toSet()
        val currentPlaylists = playlistDao.getAllPlaylists().first()

        currentPlaylists
            .filter { it.isAutoGenerated && it.name !in canonicalPlaylistNames }
            .forEach { stale ->
                playlistDao.clearPlaylist(stale.id)
                playlistDao.deletePlaylist(stale.id)
            }

        groups.values.forEach { variants ->
            val canonical = GenreUtils.getCanonicalName(variants)
            val playlistName = "$canonical - $TOP_100"
            val existingPlaylist = currentPlaylists.find { it.name == playlistName && it.isAutoGenerated }

            val playlistId = if (existingPlaylist == null) {
                val playlist = Playlist(
                    name = playlistName,
                    isAutoGenerated = true,
                    genre = canonical,
                )
                playlistDao.insertPlaylist(playlist)
                playlist.id
            } else {
                existingPlaylist.id
            }

            val topSongs = variants.keys
                .flatMap { variant -> genreDao.getTopSongsByGenre(variant) }
                .sortedWith(compareByDescending<Song> { it.rating }.thenByDescending { it.playCount })
                .take(AppConfig.Persistence.NUM_TOP_SONGS)

            playlistDao.clearPlaylist(playlistId)
            playlistDao.insertPlaylistSongs(topSongs.mapIndexed { index, song -> PlaylistSong(playlistId, song.id, index) })
        }
    }
}
