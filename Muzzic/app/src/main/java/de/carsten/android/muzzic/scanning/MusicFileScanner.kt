package de.carsten.android.muzzic.scanning

import android.content.Context
import de.carsten.android.muzzic.FLAC
import de.carsten.android.muzzic.M4A
import de.carsten.android.muzzic.MP3
import de.carsten.android.muzzic.MP4
import de.carsten.android.muzzic.OGG
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.TOP_100
import de.carsten.android.muzzic.UNKNOWN
import de.carsten.android.muzzic.UNKNOWN_ALBUM
import de.carsten.android.muzzic.UNKNOWN_ARTIST
import de.carsten.android.muzzic.UNKNOWN_GENRE
import de.carsten.android.muzzic.id3.Id3TagParser
import de.carsten.android.muzzic.logging.logger
import de.carsten.android.muzzic.model.AlbumArtUri
import de.carsten.android.muzzic.persistence.dao.GenreDao
import de.carsten.android.muzzic.persistence.dao.PlaylistDao
import de.carsten.android.muzzic.persistence.dao.SongDao
import de.carsten.android.muzzic.persistence.entity.Playlist
import de.carsten.android.muzzic.persistence.entity.PlaylistSong
import de.carsten.android.muzzic.persistence.entity.Song
import de.carsten.android.muzzic.persistence.repo.AppSettingsRepository
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO

/**
 * Implementation of [FileScanner] that scans for music files and extracts their metadata.
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
        onProgress?.invoke(context.getString(R.string.scan_status_scanning), 0)

        logger.info("Scanning for music files...")

        val configuredDir = appSettingsRepository.getMusicDirectory()
        val root = getScanningRoot(context, configuredDir) ?: return@coroutineScope
        val supportedFormats = setOf(MP3, OGG, FLAC, MP4, M4A)

        val currentSongs = songDao.getAllSongs().first()
        val currentFilePaths = currentSongs.mapNotNull { it.filePath }.toSet()

        // Count pass: determine how many files will be parsed. This is the denominator
        // for the determinate progress shown in the scan notification.
        val totalNewFiles = FileUtil.countFiles(root, supportedFormats) { it.absolutePath !in currentFilePaths }

        val existingFilePaths = mutableSetOf<String>()
        var processedFiles = 0
        val chunkSize = 20
        val pendingChunk = mutableListOf<File>()

        // Streaming pass: collect paths that still exist on disk (needed for cleanup) and
        // parse new files in overlapping chunks while the directory walk continues, so the
        // library is populated incrementally instead of all at once.
        scanForFiles(context, root, supportedFormats)
            .flowOn(Dispatchers.IO)
            .buffer(chunkSize)
            .collect { file ->
                ensureActive()
                existingFilePaths.add(file.absolutePath)
                if (file.absolutePath !in currentFilePaths) {
                    pendingChunk.add(file)
                    if (pendingChunk.size >= chunkSize) {
                        processChunk(pendingChunk.toList())
                        processedFiles += pendingChunk.size
                        reportProgress(processedFiles, totalNewFiles, onProgress)
                        pendingChunk.clear()
                    }
                }
            }

        if (pendingChunk.isNotEmpty()) {
            processChunk(pendingChunk.toList())
            processedFiles += pendingChunk.size
            reportProgress(processedFiles, totalNewFiles, onProgress)
            logger.info("... process file chunk of size ${pendingChunk.size} ($processedFiles/$totalNewFiles) ...")
        }

        if (totalNewFiles > 0) {
            updateAutomaticPlaylists()
        }

        onProgress?.invoke(context.getString(R.string.scan_status_cleaning), 100)
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

        logger.info("... done scanning for music files")
    }

    private suspend fun processChunk(chunk: List<File>) = coroutineScope {
        val songs: List<Song> =
            chunk
                .map { file ->
                    async(Dispatchers.IO) {
                        extractSongMetadata(file)
                    }
                }.awaitAll()

        withContext(Dispatchers.IO) {
            songDao.insertSongs(songs)
        }
    }

    private fun reportProgress(processedFiles: Int, totalFiles: Int, onProgress: ((String, Int) -> Unit)?) {
        val progress = (processedFiles.toFloat() / totalFiles * 100).toInt()
        onProgress?.invoke(
            context.getString(R.string.scan_status_metadata, progress),
            progress,
        )
    }

    private fun extractSongMetadata(file: File): Song = try {
        val audioFile = AudioFileIO.read(file)
        val extractedMetadata = Id3TagParser.extractMetadata(audioFile)

        Song.fromId3(file, extractedMetadata, saveAlbumArt(file))
    } catch (e: Exception) {
        logger.error("Failed to extract metadata for ${file.absolutePath}", e)
        Song(
            title = UNKNOWN,
            artist = UNKNOWN_ARTIST,
            album = UNKNOWN_ALBUM,
            genre = UNKNOWN_GENRE,
            duration = 0L,
            filePath = file.absolutePath,
        )
    }

    private fun saveAlbumArt(file: File): String? = try {
        val filePath = file.absolutePath
        val albumArtOffset = Id3TagParser.getAlbumArtMetadata(file)
        if (albumArtOffset.isValid) {
            AlbumArtUri(filePath, albumArtOffset.offset, albumArtOffset.size, albumArtOffset.hashCode).get()
        } else {
            logger.debug("No valid album art offset found for $filePath")
            null
        }
    } catch (e: Exception) {
        logger.error("An error occurred while saving album art for $file", e)
        null
    }

    suspend fun updateAutomaticPlaylists() = withContext(Dispatchers.IO) {
        val genres = genreDao.getAllGenres()
        val currentPlaylists = playlistDao.getAllPlaylists().first()

        genres.forEach { genre ->
            val playlistName = "$genre - $TOP_100"
            val existingPlaylist = currentPlaylists.find { it.name == playlistName && it.isAutoGenerated }

            val playlistId =
                if (existingPlaylist == null) {
                    val playlist =
                        Playlist(
                            name = playlistName,
                            isAutoGenerated = true,
                            genre = genre,
                        )
                    playlistDao.insertPlaylist(playlist)
                    playlist.id
                } else {
                    existingPlaylist.id
                }

            val topSongs = genreDao.getTopSongsByGenre(genre, 100)
            playlistDao.clearPlaylist(playlistId)
            topSongs.forEachIndexed { index, song ->
                playlistDao.insertPlaylistSong(
                    PlaylistSong(playlistId, song.id, index),
                )
            }
        }
    }
}
