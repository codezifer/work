package de.carsten.android.muzzic.scanning

import android.content.Context
import android.media.MediaMetadataRetriever
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
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File

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
        val musicFiles = scanForMusicFiles()
        val currentSongs = songDao.getAllSongs().first()
        val currentFilePaths = currentSongs.mapNotNull { it.filePath }.toSet()

        val newFiles = musicFiles.filter { it.absolutePath !in currentFilePaths }

        if (newFiles.isNotEmpty()) {
            val totalFiles = newFiles.size
            var processedFiles = 0
            val chunkSize = 20
            val chunkedFiles = newFiles.chunked(chunkSize)

            for (chunk in chunkedFiles) {
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
        val existingFilesOnDisk = musicFiles.map { it.absolutePath }.toSet()
        val missingSongs =
            currentSongs.filter { song ->
                val path = song.filePath
                path != null &&
                    !path.startsWith("content://mock") &&
                    !path.startsWith("http://") &&
                    !path.startsWith("https://") &&
                    path !in existingFilesOnDisk
            }

        if (missingSongs.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                songDao.deleteSongs(missingSongs)
            }
        }
    }

    private suspend fun scanForMusicFiles(): List<File> = withContext(Dispatchers.IO) {
        val configuredDir = appSettingsRepository.getMusicDirectory()
        val musicFolders = contentToFiles(configuredDir)
        if (musicFolders.isEmpty()) return@withContext emptyList()

        val supportedFormats = setOf(MP3, OGG, FLAC, MP4, M4A)
        val musicFiles = mutableListOf<File>()

        musicFolders.forEach { folder ->
            ensureActive()
            if (folder.exists()) {
                folder
                    .walkTopDown()
                    .onEnter {
                        ensureActive()
                        true
                    }.filter { it.isFile && it.extension.lowercase() in supportedFormats }
                    .forEach {
                        ensureActive()
                        musicFiles.add(it)
                    }
            }
        }

        musicFiles
    }

    private fun extractSongMetadata(file: File): Song = try {
        val audioFile = AudioFileIO.read(file)
        val tag = audioFile.tag
        val extended = Id3TagParser.extractMetadata(audioFile)

        Song(
            title = tag?.getFirst(FieldKey.TITLE)?.trim() ?: file.nameWithoutExtension,
            artist = tag?.getFirst(FieldKey.ARTIST)?.trim() ?: UNKNOWN_ARTIST,
            album = tag?.getFirst(FieldKey.ALBUM)?.trim() ?: UNKNOWN_ALBUM,
            genre = tag?.getFirst(FieldKey.GENRE)?.trim() ?: UNKNOWN_GENRE,
            duration = audioFile.audioHeader.trackLength.toLong() * 1000L,
            filePath = file.absolutePath,
            albumArt = saveAlbumArt(file),
            albumYear = extended.year,
            trackNumber = extended.trackNumber.coerceAtLeast(0),
            totalTracks = extended.totalTracks.coerceAtLeast(0),
            rating = extended.rating,
            playCount = extended.playCount,
        )
    } catch (e: Exception) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)

            val trackString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
            val (trackNumber, totalTracks) = Id3TagParser.parseTrackString(trackString)
            val yearString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)

            Song(
                title =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.trim()
                        ?: file.nameWithoutExtension,
                artist =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.trim()
                        ?: UNKNOWN_ARTIST,
                album =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.trim()
                        ?: UNKNOWN_ALBUM,
                genre =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)?.trim()
                        ?: UNKNOWN_GENRE,
                duration =
                    retriever
                        .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull() ?: 0L,
                filePath = file.absolutePath,
                albumArt = saveAlbumArt(file),
                trackNumber = trackNumber.coerceAtLeast(0),
                totalTracks = totalTracks.coerceAtLeast(0),
                albumYear = Id3TagParser.parseId3Year(yearString),
            )
        } catch (e: Exception) {
            Song(
                title = file.nameWithoutExtension,
                artist = UNKNOWN_ARTIST,
                album = UNKNOWN_ALBUM,
                genre = UNKNOWN_GENRE,
                duration = 0L,
                filePath = file.absolutePath,
            )
        } finally {
            retriever.release()
        }
    }

    private fun saveAlbumArt(file: File): String? = try {
        val albumArtOffset = Id3TagParser.getAlbumArtMetadata(file)
        if (albumArtOffset.isValid) {
            AlbumArtUri(file.absolutePath, albumArtOffset.offset, albumArtOffset.size, albumArtOffset.hashCode).get()
        } else {
            logger.debug("No valid album art offset found for ${file.name}")
            null
        }
    } catch (e: Exception) {
        logger.error("An error occurred while saving album art for ${file.name}", e)
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
