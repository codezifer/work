package de.carsten.android.muzzic.persistence.repo

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Environment
import com.mpatric.mp3agic.BufferTools
import com.mpatric.mp3agic.ID3v2ObseletePictureFrameData
import com.mpatric.mp3agic.ID3v2PictureFrameData
import com.mpatric.mp3agic.ID3v2TagWithOffset
import com.mpatric.mp3agic.Mp3File
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
import de.carsten.android.muzzic.ui.utils.parseId3Year
import de.carsten.android.muzzic.utils.flac
import de.carsten.android.muzzic.utils.m4a
import de.carsten.android.muzzic.utils.mp3
import de.carsten.android.muzzic.utils.mp4
import de.carsten.android.muzzic.utils.ogg
import de.carsten.android.muzzic.utils.top100
import de.carsten.android.muzzic.utils.unknownAlbum
import de.carsten.android.muzzic.utils.unknownArtist
import de.carsten.android.muzzic.utils.unknownGenre
import kotlinx.coroutines.flow.first
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
    }

    fun getAllSongs() = songDao.getAllSongs()

    fun getAllPlaylists() = playlistDao.getAllPlaylists()

    suspend fun scanMusicLibrary() {
        val musicFiles = scanForMusicFiles()
        val currentSongs = songDao.getAllSongs().first()
        val currentFilePaths = currentSongs.mapNotNull { it.filePath }.toSet()

        val newFiles = musicFiles.filter { it.absolutePath !in currentFilePaths }

        if (newFiles.isNotEmpty()) {
            val songs = newFiles.map { file ->
                extractSongMetadata(file)
            }
            songDao.insertSongs(songs)
            generateAutomaticPlaylists()
        }

        // Clean up songs that no longer exist on disk, BUT keep mock songs
        val existingFilesOnDisk = musicFiles.map { it.absolutePath }.toSet()
        val missingSongs = currentSongs.filter { song ->
            val path = song.filePath
            path != null && !path.startsWith("content://mock") && path !in existingFilesOnDisk
        }

        if (missingSongs.isNotEmpty()) {
            songDao.deleteSongs(missingSongs)
        }
    }

    private fun scanForMusicFiles(): List<File> {
        val musicFolders =
            listOf(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            )

        val supportedFormats = setOf(mp3, ogg, flac, mp4, m4a)
        val musicFiles = mutableListOf<File>()

        musicFolders.forEach { folder ->
            if (folder.exists()) {
                folder
                    .walkTopDown()
                    .filter { it.isFile && it.extension.lowercase() in supportedFormats }
                    .forEach { musicFiles.add(it) }
            }
        }

        return musicFiles
    }

    private fun extractSongMetadata(file: File): Song =
        try {
            if (file.extension.lowercase() == mp3) {
                val mp3file = Mp3File(file)
                val id3v2Tag = mp3file.id3v2Tag

                Song(
                    title = id3v2Tag?.title ?: file.nameWithoutExtension,
                    artist = id3v2Tag?.artist ?: unknownArtist,
                    album = id3v2Tag?.album ?: unknownAlbum,
                    genre = id3v2Tag?.genreDescription ?: unknownGenre,
                    duration = mp3file.lengthInMilliseconds,
                    filePath = file.absolutePath,
                    albumArt = saveAlbumArt(file),
                    albumYear = parseId3Year(id3v2Tag?.year),
                    rating = id3v2Tag?.wmpRating ?: 0,
                )
            } else {
                // For other formats, use MediaMetadataRetriever
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(file.absolutePath)

                Song(
                    title =
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                            ?: file.nameWithoutExtension,
                    artist =
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                            ?: unknownArtist,
                    album =
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                            ?: unknownAlbum,
                    genre =
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
                            ?: unknownGenre,
                    duration =
                        retriever
                            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                            ?.toLongOrNull() ?: 0L,
                    filePath = file.absolutePath,
                )
            }
        } catch (e: Exception) {
            // Fallback
            Song(
                title = file.nameWithoutExtension,
                artist = unknownArtist,
                album = unknownAlbum,
                genre = unknownGenre,
                duration = 0L,
                filePath = file.absolutePath,
            )
        }

    private fun saveAlbumArt(
        file: File,
    ): String? = try {
        val (offset, size) = getAlbumArtOffsetAndSize(file)
        AlbumArtUri(file.absolutePath, offset, size).get()
    } catch (e: Exception) {
        logger.error("An error occurred while saving album art for ${file.name}", e)
        null
    }

    private suspend fun generateAutomaticPlaylists() {
        val genres = genreDao.getAllGenres()

        genres.forEach { genre ->
            val existingPlaylist =
                playlistDao
                    .getAllPlaylists()
                    .first()
                    .find { it.name == "$genre - $top100" && it.isAutoGenerated }

            if (existingPlaylist == null) {
                val playlist =
                    Playlist(
                        name = "$genre - $top100",
                        isAutoGenerated = true,
                        genre = genre,
                    )
                val playlistId = playlistDao.insertPlaylist(playlist).toString()

                val topSongs = genreDao.getTopSongsByGenre(genre, 100)
                topSongs.forEachIndexed { index, song ->
                    playlistDao.insertPlaylistSong(
                        PlaylistSong(playlistId, song.id, index),
                    )
                }
            }
        }
    }

    suspend fun recordPlay(songId: String) {
        songDao.incrementPlayCount(songId)
        playHistoryDao.insertPlayHistory(PlayHistory(songId = songId))
    }

    suspend fun updateSongRating(
        songId: String,
        rating: Int,
    ) {
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
        return playHistoryDao.getTopSongs(oneMonthAgo, 50)
    }

    private fun getAlbumArtOffsetAndSize(file: File): Pair<Long, Long> {
        return try {
            file.inputStream().use { input ->
                val header = ByteArray(10)
                if (input.read(header) != 10 || String(header, 0, 3) != "ID3") return Pair(0L, 0L)
                // Get tag length (synchsafe integer at offset 6)
                val tagLength = BufferTools.unpackSynchsafeInteger(header[6], header[7], header[8], header[9])
                val tagBytes = ByteArray(tagLength + 10)
                System.arraycopy(header, 0, tagBytes, 0, 10)
                input.read(tagBytes, 10, tagLength)

                val offsetTag = ID3v2TagWithOffset(tagBytes)
                val apicFrame = offsetTag.getApicFrame() ?: return Pair(0L, 0L)

                // The image data starts inside the frame data.
                // We use mp3agic's own PictureFrameData to calculate the internal offset.
                val pictureData = if (apicFrame.id == "PIC") {
                    ID3v2ObseletePictureFrameData(false, apicFrame.data)
                } else {
                    ID3v2PictureFrameData(false, apicFrame.data)
                }

                // Calculation:
                // Frame Offset in Tag + Frame Header (10 bytes) + Header fields inside APIC
                val internalOffset = apicFrame.data.size - pictureData.imageData.size
                val finalOffset = apicFrame.offsetInTag.toLong() + 10L + internalOffset.toLong()
                val size = pictureData.imageData.size.toLong()

                Pair(finalOffset, size)
            }
        } catch (e: Exception) {
            Pair(0L, 0L)
        }
    }

    private fun indexOf(data: ByteArray, search: ByteArray): Int {
        if (search.isEmpty()) return 0
        startloop@ for (i in 0 until data.size - search.size + 1) {
            for (j in search.indices) {
                if (data[i + j] != search[j]) continue@startloop
            }
            return i
        }
        return -1
    }
}
