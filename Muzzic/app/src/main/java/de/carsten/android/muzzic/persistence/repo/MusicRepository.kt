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
import de.carsten.android.muzzic.persistence.entity.aggregation.LibrarySummary
import de.carsten.android.muzzic.persistence.entity.aggregation.MonthlyPlayCount
import de.carsten.android.muzzic.persistence.entity.aggregation.SongPlayCount
import de.carsten.android.muzzic.scanning.MusicFileScanner
import de.carsten.android.muzzic.scanning.PlaylistFileScanner
import de.carsten.android.muzzic.ui.model.SongDto
import java.time.YearMonth
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class MusicRepository(
    val songDao: SongDao,
    val playHistoryDao: PlayHistoryDao,
    private val playlistDao: PlaylistDao,
    private val playingQueueDao: PlayingQueueDao,
    private val database: MuzzicDatabase,
    val context: Context,
    private val musicFileScanner: MusicFileScanner,
    private val playlistFileScanner: PlaylistFileScanner,
    private val genreRepository: GenreRepository,
) {
    companion object {
        private const val ONE_YEAR_MS = 31536000000L // one year in milliseconds
    }

    fun getAllSongs(): Flow<List<SongDto>> = songDao.getAllSongs()
        .distinctUntilChanged()
        .map { list -> list.map { song -> song.toDto() } }

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

    /**
     * Returns top genres by play count with canonical genre names.
     *
     * Spelling variants are merged before the limit is applied, so variants below
     * the cutoff still count towards their canonical genre.
     *
     * @return top genres ordered by play count descending.
     */
    suspend fun getGenreStats(): List<GenrePlayCount> {
        val oneYearAgo = System.currentTimeMillis() - ONE_YEAR_MS
        val rawStats = songDao.getGenreStats(oneYearAgo, UNKNOWN)
        return genreRepository.getCanonicalGenreStats(rawStats).take(AppConfig.Ui.NUM_OF_TOP_GENRES)
    }

    suspend fun getTopSongs(): List<SongPlayCount> = songDao.getTopSongs(AppConfig.Ui.NUM_OF_TOP_SONGS)

    suspend fun updateAutomaticPlaylists() {
        musicFileScanner.updateAutomaticPlaylists()
    }

    suspend fun getMonthStats(): List<MonthlyPlayCount> = playHistoryDao.getMonthlyStats(startOfCurrentMonth(), AppConfig.Ui.NUM_OF_MONTHS)

    /**
     * Returns this month's genres by play count with canonical genre names.
     *
     * @return genres ordered by play count descending.
     */
    suspend fun getMonthGenreStats(): List<GenrePlayCount> = genreRepository.getCanonicalGenreStats(playHistoryDao.getGenreStats(startOfCurrentMonth()))

    suspend fun getTopMonthSongs(): List<SongPlayCount> = playHistoryDao.getTopSongs(startOfCurrentMonth(), AppConfig.Ui.NUM_OF_TOP_SONGS)

    suspend fun getLibrarySummary(): LibrarySummary = songDao.getLibrarySummary()

    private fun startOfCurrentMonth(): Long = YearMonth.now().atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}
