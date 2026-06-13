package de.carsten.android.muzzic.persistence.repo

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Environment
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.id3.Id3TagParser
import de.carsten.android.muzzic.logging.logger
import de.carsten.android.muzzic.model.AlbumArtUri
import de.carsten.android.muzzic.persistence.dao.AlbumDao
import de.carsten.android.muzzic.persistence.dao.ArtistDao
import de.carsten.android.muzzic.persistence.dao.GenreDao
import de.carsten.android.muzzic.persistence.dao.PlayHistoryDao
import de.carsten.android.muzzic.persistence.dao.PlaylistDao
import de.carsten.android.muzzic.persistence.dao.SongDao
import de.carsten.android.muzzic.persistence.entity.PlayHistory
import de.carsten.android.muzzic.persistence.entity.Playlist
import de.carsten.android.muzzic.persistence.entity.PlaylistSong
import de.carsten.android.muzzic.persistence.entity.Song
import de.carsten.android.muzzic.persistence.entity.aggregation.GenrePlayCount
import de.carsten.android.muzzic.persistence.entity.aggregation.MonthlyPlayCount
import de.carsten.android.muzzic.persistence.entity.aggregation.SongPlayCount
import de.carsten.android.muzzic.utils.FLAC
import de.carsten.android.muzzic.utils.M4A
import de.carsten.android.muzzic.utils.MP3
import de.carsten.android.muzzic.utils.MP4
import de.carsten.android.muzzic.utils.OGG
import de.carsten.android.muzzic.utils.TOP_100
import de.carsten.android.muzzic.utils.UNKNOWN_ALBUM
import de.carsten.android.muzzic.utils.UNKNOWN_ARTIST
import de.carsten.android.muzzic.utils.UNKNOWN_GENRE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

class MusicRepository(
    val songDao: SongDao,
    val artistDao: ArtistDao,
    val albumDao: AlbumDao,
    val genreDao: GenreDao,
    val playlistDao: PlaylistDao,
    val playHistoryDao: PlayHistoryDao,
    val context: Context,
) {
    companion object {
        @JvmStatic
        private val logger = logger()
        const val SCAN_WORK_NAME = "MusicLibraryScanWork"
    }

    fun getAllSongs() = songDao.getAllSongs()

    suspend fun scanMusicLibrary() {
        try {
            val workManager = WorkManager.getInstance(context)
            val scanRequest =
                OneTimeWorkRequestBuilder<MusicScanWorker>()
                    .setConstraints(
                        Constraints
                            .Builder()
                            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                            .setRequiresBatteryNotLow(true)
                            .build(),
                    ).addTag(SCAN_WORK_NAME)
                    .build()

            workManager.enqueueUniqueWork(
                SCAN_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                scanRequest,
            )
        } catch (e: Exception) {
            logger.error("Failed to enqueue music scan work", e)
            // Fallback to immediate scan if WorkManager fails
            performLibraryScan()
        }
    }

    suspend fun performLibraryScan(onProgress: ((String, Int) -> Unit)? = null) = coroutineScope {
        onProgress?.invoke(context.getString(R.string.scan_status_scanning), 0)
        val musicFiles = scanForMusicFiles()
        val currentSongs = songDao.getAllSongs().first()
        val currentFilePaths = currentSongs.mapNotNull { it.filePath }.toSet()

        val newFiles = musicFiles.filter { it.absolutePath !in currentFilePaths }

        if (newFiles.isNotEmpty()) {
            // Process metadata extraction in parallel using IO dispatcher
            // Limit parallelism to avoid overwhelming the system
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
        val musicFolders =
            listOf(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            )

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
        // Fallback to MediaMetadataRetriever if JAudioTagger fails
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
        val albumArtOffset = Id3TagParser.getAlbumArtOffsetAndSize(file)
        if (albumArtOffset.isValid) {
            AlbumArtUri(file.absolutePath, albumArtOffset.offset, albumArtOffset.size).get()
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

            // Update content
            val topSongs = genreDao.getTopSongsByGenre(genre, 100)
            playlistDao.clearPlaylist(playlistId)
            topSongs.forEachIndexed { index, song ->
                playlistDao.insertPlaylistSong(
                    PlaylistSong(playlistId, song.id, index),
                )
            }
        }
    }

    suspend fun recordPlay(songId: String) {
        songDao.incrementPlayCount(songId)
        playHistoryDao.insertPlayHistory(PlayHistory(songId))
    }

    suspend fun updateSongRating(songId: String, rating: Int) {
        songDao.updateRating(songId, rating)
    }

    suspend fun getMonthlyStats(): List<MonthlyPlayCount> {
        val sixMonthsAgo = System.currentTimeMillis() - (6 * 30 * 24 * 60 * 60 * 1000L)
        return playHistoryDao.getMonthlyStats(sixMonthsAgo)
    }

    suspend fun getGenreStats(): List<GenrePlayCount> {
        val oneYearAgo = System.currentTimeMillis() - (365 * 24 * 60 * 60 * 1000L)
        return playHistoryDao.getGenreStats(oneYearAgo)
    }

    suspend fun getTopSongs(): List<SongPlayCount> {
        val oneMonthAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
        return playHistoryDao.getTopSongs(oneMonthAgo)
    }
}
