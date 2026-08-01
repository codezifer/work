package de.carsten.android.muzzic.persistence.repo

import android.content.Context
import de.carsten.android.muzzic.AppConfig
import de.carsten.android.muzzic.UNKNOWN
import de.carsten.android.muzzic.persistence.dao.PlayHistoryDao
import de.carsten.android.muzzic.persistence.dao.SongDao
import de.carsten.android.muzzic.persistence.entity.PlayHistory
import de.carsten.android.muzzic.persistence.entity.aggregation.GenrePlayCount
import de.carsten.android.muzzic.persistence.entity.aggregation.MonthlyPlayCount
import de.carsten.android.muzzic.persistence.entity.aggregation.SongPlayCount
import de.carsten.android.muzzic.scanning.MusicFileScanner
import de.carsten.android.muzzic.scanning.PlaylistFileScanner
import java.time.YearMonth
import java.time.ZoneOffset

class MusicRepository(
    val songDao: SongDao,
    val playHistoryDao: PlayHistoryDao,
    val context: Context,
    private val musicFileScanner: MusicFileScanner,
    private val playlistFileScanner: PlaylistFileScanner,
) {
    companion object {
        private const val ONE_YEAR_MS = 31536000000L // one year in milliseconds
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
        val oneYearAgo = System.currentTimeMillis() - ONE_YEAR_MS
        return songDao.getMonthlyStats(oneYearAgo, AppConfig.Ui.NUM_OF_MONTHS)
    }

    suspend fun getGenreStats(): List<GenrePlayCount> {
        val oneYearAgo = System.currentTimeMillis() - ONE_YEAR_MS
        return songDao.getGenreStats(oneYearAgo, AppConfig.Ui.NUM_OF_TOP_GENRES, UNKNOWN)
    }

    suspend fun getTopSongs(): List<SongPlayCount> = songDao.getTopSongs(AppConfig.Ui.NUM_OF_TOP_SONGS)

    suspend fun updateAutomaticPlaylists() {
        musicFileScanner.updateAutomaticPlaylists()
    }

    suspend fun getMonthStats(): List<MonthlyPlayCount> = playHistoryDao.getMonthlyStats(startOfCurrentMonth(), AppConfig.Ui.NUM_OF_MONTHS)

    suspend fun getMonthGenreStats(): List<GenrePlayCount> = playHistoryDao.getGenreStats(startOfCurrentMonth())

    suspend fun getTopMonthSongs(): List<SongPlayCount> = playHistoryDao.getTopSongs(startOfCurrentMonth(), AppConfig.Ui.NUM_OF_TOP_SONGS)

    private fun startOfCurrentMonth(): Long = YearMonth.now().atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}
