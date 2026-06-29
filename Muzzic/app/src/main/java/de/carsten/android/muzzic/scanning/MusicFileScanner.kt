package de.carsten.android.muzzic.scanning

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import de.carsten.android.muzzic.FLAC
import de.carsten.android.muzzic.M4A
import de.carsten.android.muzzic.MP3
import de.carsten.android.muzzic.MP4
import de.carsten.android.muzzic.OGG
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.TOP_100
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

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

    override val scannerId: String = "MusicScanner"
    override val notificationTitleRes: Int = R.string.scan_notification_title

    companion object {
        private val logger = logger()
    }

    override suspend fun scan(onProgress: ((String, Int) -> Unit)?) = coroutineScope {
        onProgress?.invoke(context.getString(R.string.scan_status_scanning), 0)

        val configuredDir = appSettingsRepository.getMusicDirectory()
        val roots = getScanningRoots(configuredDir)
        val supportedFormats = setOf(MP3, OGG, FLAC, MP4, M4A)

        val musicFileUris = scanForFiles(context, roots, supportedFormats)

        val currentSongs = songDao.getAllSongs().first()
        val currentFilePaths = currentSongs.mapNotNull { it.filePath }.toSet()

        val newFileUris = musicFileUris.filter { it !in currentFilePaths }

        if (newFileUris.isNotEmpty()) {
            val totalFiles = newFileUris.size
            var processedFiles = 0
            val chunkSize = 20
            val chunkedFiles = newFileUris.chunked(chunkSize)

            for (chunk in chunkedFiles) {
                val songs: List<Song> =
                    chunk
                        .map { uri ->
                            async(Dispatchers.IO) {
                                extractSongMetadata(uri)
                            }
                        }.awaitAll()

                withContext(Dispatchers.IO) {
                    songDao.insertSongs(songs)
                }

                processedFiles += chunk.size
                val progress = (processedFiles.toFloat() / totalFiles * 100).toInt()
                onProgress?.invoke(
                    context.getString(R.string.scan_status_metadata, progress),
                    progress,
                )
            }
            updateAutomaticPlaylists()
        }

        onProgress?.invoke(context.getString(R.string.scan_status_cleaning), 100)
        ensureActive()

        // Clean up songs that no longer exist on disk, BUT keep mock songs and remote URLs
        val existingUris = musicFileUris.toSet()
        val missingSongs =
            currentSongs.filter { song ->
                val path = song.filePath
                path != null &&
                    !path.startsWith("content://mock") &&
                    !path.startsWith("http://") &&
                    !path.startsWith("https://") &&
                    path !in existingUris
            }

        if (missingSongs.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                songDao.deleteSongs(missingSongs)
            }
        }
    }

    private fun extractSongMetadata(uriString: String): Song = try {
        val uri = uriString.toUri()
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)

            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.trim()
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.trim() ?: UNKNOWN_ARTIST
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.trim() ?: UNKNOWN_ALBUM
            val genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)?.trim() ?: UNKNOWN_GENRE
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val trackString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
            val (trackNumber, totalTracks) = Id3TagParser.parseTrackString(trackString)
            val yearString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)

            val extended = Id3TagParser.extractExtendedMetadata(null) // TODO: Could improve Id3TagParser to extract from Stream if needed

            Song(
                title = title ?: DocumentFile.fromSingleUri(context, uri)?.name?.substringBeforeLast('.') ?: "Unknown",
                artist = artist,
                album = album,
                genre = genre,
                duration = duration,
                filePath = uriString,
                albumArt = saveAlbumArt(uriString),
                trackNumber = trackNumber.coerceAtLeast(0),
                totalTracks = totalTracks.coerceAtLeast(0),
                albumYear = Id3TagParser.parseId3Year(yearString),
                rating = extended.rating,
                playCount = extended.playCount,
            )
        } finally {
            retriever.release()
        }
    } catch (e: Exception) {
        logger.error("Failed to extract metadata for $uriString", e)
        Song(
            title = DocumentFile.fromSingleUri(context, Uri.parse(uriString))?.name?.substringBeforeLast('.') ?: "Unknown",
            artist = UNKNOWN_ARTIST,
            album = UNKNOWN_ALBUM,
            genre = UNKNOWN_GENRE,
            duration = 0L,
            filePath = uriString,
        )
    }

    private fun saveAlbumArt(uriString: String): String? = try {
        val albumArtOffset = Id3TagParser.getAlbumArtMetadata(context, uriString)
        if (albumArtOffset.isValid) {
            AlbumArtUri(uriString, albumArtOffset.offset, albumArtOffset.size, albumArtOffset.hashCode).get()
        } else {
            logger.debug("No valid album art offset found for $uriString")
            null
        }
    } catch (e: Exception) {
        logger.error("An error occurred while saving album art for $uriString", e)
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
