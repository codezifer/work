package de.carsten.android.muzzic.persistence.repo

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Environment
import com.mpatric.mp3agic.Mp3File
import de.carsten.android.muzzic.persistence.dao.PlayHistoryDao
import de.carsten.android.muzzic.persistence.dao.PlaylistDao
import de.carsten.android.muzzic.persistence.dao.SongDao
import de.carsten.android.muzzic.persistence.entity.GenrePlayCount
import de.carsten.android.muzzic.persistence.entity.MonthlyPlayCount
import de.carsten.android.muzzic.persistence.entity.PlayHistory
import de.carsten.android.muzzic.persistence.entity.Playlist
import de.carsten.android.muzzic.persistence.entity.PlaylistSong
import de.carsten.android.muzzic.persistence.entity.Song
import de.carsten.android.muzzic.persistence.entity.SongPlayCount
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
import org.koin.android.logger.AndroidLogger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

class MusicRepository(
    val songDao: SongDao,
    val playlistDao: PlaylistDao,
    val playHistoryDao: PlayHistoryDao,
    val context: Context
) : KoinComponent {
    private val logger: AndroidLogger by inject()

    companion object {
        private val TAG = MusicRepository::class.toString()
    }

    fun getAllSongs() = songDao.getAllSongs()
    fun getAllPlaylists() = playlistDao.getAllPlaylists()

    suspend fun scanMusicLibrary() {
        val musicFiles = scanForMusicFiles()
        val songs = musicFiles.map { file ->
            extractSongMetadata(file)
        }
        songDao.insertSongs(songs)
        generateAutomaticPlaylists()
    }

    private fun scanForMusicFiles(): List<File> {
        val musicFolders = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        )

        val supportedFormats = setOf(mp3, ogg, flac, mp4, m4a)
        val musicFiles = mutableListOf<File>()

        musicFolders.forEach { folder ->
            folder.walkTopDown()
                .filter { it.isFile && it.extension.lowercase() in supportedFormats }
                .forEach { musicFiles.add(it) }
        }

        return musicFiles
    }

    private fun extractSongMetadata(file: File): Song {
        return try {
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
                    albumArt = id3v2Tag?.albumImage?.let {
                        saveAlbumArt(it, file.nameWithoutExtension)
                    },
                    rating = id3v2Tag.wmpRating
                )
            } else {
                // For other formats, use MediaMetadataRetriever
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(file.absolutePath)

                Song(
                    title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                        ?: file.nameWithoutExtension,
                    artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                        ?: unknownArtist,
                    album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                        ?: unknownAlbum,
                    genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
                        ?: unknownGenre,
                    duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
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
                filePath = file.absolutePath
            )
        }
    }

    private fun saveAlbumArt(imageData: ByteArray, fileName: String): String? {
        return try {
            val albumArtDir = File(context.filesDir, "album_art")
            if (!albumArtDir.exists()) albumArtDir.mkdirs()

            val artFile = File(albumArtDir, "$fileName.jpg")
            artFile.writeBytes(imageData)
            artFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun generateAutomaticPlaylists() {
        val genres = songDao.getAllGenres()

        genres.forEach { genre ->
            val existingPlaylist = playlistDao.getAllPlaylists().first()
                .find { it.name == "$genre - $top100" && it.isAutoGenerated }

            if (existingPlaylist == null) {
                val playlist = Playlist(
                    name = "$genre - $top100",
                    isAutoGenerated = true,
                    genre = genre
                )
                val playlistId = playlistDao.insertPlaylist(playlist).toString()

                val topSongs = songDao.getTopSongsByGenre(genre, 100)
                topSongs.forEachIndexed { index, song ->
                    playlistDao.insertPlaylistSong(
                        PlaylistSong(playlistId, song.id, index)
                    )
                }
            }
        }
    }

    suspend fun recordPlay(songId: String) {
        songDao.incrementPlayCount(songId)
        playHistoryDao.insertPlayHistory(PlayHistory(songId = songId))
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
        return playHistoryDao.getTopSongs(oneMonthAgo, 50)
    }
}
