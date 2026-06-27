package de.carsten.android.muzzic.persistence.repo

import android.content.Context
import de.carsten.android.muzzic.logging.logger
import de.carsten.android.muzzic.persistence.dao.PlayHistoryDao
import de.carsten.android.muzzic.persistence.dao.SongDao
import de.carsten.android.muzzic.persistence.entity.PlayHistory
import de.carsten.android.muzzic.persistence.entity.aggregation.GenrePlayCount
import de.carsten.android.muzzic.persistence.entity.aggregation.MonthlyPlayCount
import de.carsten.android.muzzic.persistence.entity.aggregation.SongPlayCount
import de.carsten.android.muzzic.scanning.MusicFileScanner
import de.carsten.android.muzzic.scanning.PlaylistFileScanner

class MusicRepository(
    val songDao: SongDao,
    val playHistoryDao: PlayHistoryDao,
    val context: Context,
    private val musicFileScanner: MusicFileScanner,
    private val playlistFileScanner: PlaylistFileScanner,
) {
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

    suspend fun updateAutomaticPlaylists() {
        musicFileScanner.updateAutomaticPlaylists()
    }
}
