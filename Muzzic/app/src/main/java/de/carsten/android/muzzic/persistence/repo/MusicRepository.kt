package de.carsten.android.muzzic.persistence.repo

import android.content.Context
import de.carsten.android.muzzic.AppConfig
import de.carsten.android.muzzic.UNKNOWN
import de.carsten.android.muzzic.logging.logger
import de.carsten.android.muzzic.persistence.dao.SongDao
import de.carsten.android.muzzic.persistence.entity.aggregation.GenrePlayCount
import de.carsten.android.muzzic.persistence.entity.aggregation.MonthlyPlayCount
import de.carsten.android.muzzic.persistence.entity.aggregation.SongPlayCount
import de.carsten.android.muzzic.scanning.MusicFileScanner
import de.carsten.android.muzzic.scanning.PlaylistFileScanner

class MusicRepository(val songDao: SongDao, val context: Context, private val musicFileScanner: MusicFileScanner, private val playlistFileScanner: PlaylistFileScanner) {
    companion object {
        @JvmStatic
        private val logger = logger()
    }

    fun getAllSongs() = songDao.getAllSongs()

    fun scanMusicLibrary() {
        musicFileScanner.enqueue(context)
    }

    fun importPlaylists() {
        playlistFileScanner.enqueue(context)
    }

    suspend fun recordPlay(songId: String) {
        songDao.incrementPlayCount(songId)
    }

    suspend fun updateSongRating(songId: String, rating: Int) {
        songDao.updateRating(songId, rating)
    }

    suspend fun getMonthlyStats(): List<MonthlyPlayCount> {
        val oneYearAgo = System.currentTimeMillis() - (365 * 24 * 60 * 60 * 1000L)
        return songDao.getMonthlyStats(oneYearAgo).takeLast(12)
    }

    suspend fun getGenreStats(): List<GenrePlayCount> {
        val oneYearAgo = System.currentTimeMillis() - (365 * 24 * 60 * 60 * 1000L)
        return songDao.getGenreStats(oneYearAgo, AppConfig.Ui.NUM_OF_TOP_GENRES, UNKNOWN)
    }

    suspend fun getTopSongs(): List<SongPlayCount> = songDao.getTopSongs(AppConfig.Ui.NUM_OF_TOP_SONGS)

    suspend fun updateAutomaticPlaylists() {
        musicFileScanner.updateAutomaticPlaylists()
    }
}
