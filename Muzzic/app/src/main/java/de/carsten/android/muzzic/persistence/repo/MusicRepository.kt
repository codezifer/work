package de.carsten.android.muzzic.persistence.repo

import android.content.Context
import androidx.room.withTransaction
import de.carsten.android.muzzic.AppConfig
import de.carsten.android.muzzic.UNKNOWN
import de.carsten.android.muzzic.persistence.MuzzicDatabase
import de.carsten.android.muzzic.persistence.dao.PlayHistoryDao
import de.carsten.android.muzzic.persistence.dao.PlayingQueueDao
import de.carsten.android.muzzic.persistence.dao.PlaylistDao
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
    private val playlistDao: PlaylistDao,
    private val playingQueueDao: PlayingQueueDao,
    private val database: MuzzicDatabase,
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

    /**
     * Clears the entire library: songs, playlists, play history and the playing queue.
     *
     * Playlist-song assignments are removed via foreign key cascade; the queue is cleared
     * explicitly because its [de.carsten.android.muzzic.persistence.entity.PlayingQueue]
     * entries may reference no song at all. Runs atomically so a failure cannot leave
     * the library half-cleared. App settings (e.g. configured directories)
     * are kept so the library can be rescanned fresh.
     */
    suspend fun clearLibrary() = database.withTransaction {
        songDao.deleteAllSongs()
        playlistDao.deleteAllPlaylists()
        playHistoryDao.deleteAllPlayHistory()
        playingQueueDao.clearQueue()
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
