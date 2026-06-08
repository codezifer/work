package de.carsten.android.muzzic.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import de.carsten.android.muzzic.AppConfig
import de.carsten.android.muzzic.persistence.entity.PlayHistory
import de.carsten.android.muzzic.persistence.entity.aggregation.GenrePlayCount
import de.carsten.android.muzzic.persistence.entity.aggregation.MonthlyPlayCount
import de.carsten.android.muzzic.persistence.entity.aggregation.SongPlayCount

@Dao
interface PlayHistoryDao {
    @Query(
        "SELECT COUNT(*) as count, strftime('%Y-%m', datetime(playedAt/1000, 'unixepoch')) as month FROM play_history WHERE playedAt >= :fromTimestamp GROUP BY month ORDER BY month",
    )
    suspend fun getMonthlyStats(fromTimestamp: Long): List<MonthlyPlayCount>

    @Query(
        "SELECT s.genre, COUNT(*) as count FROM play_history ph INNER JOIN songs s ON ph.songId = s.id WHERE ph.playedAt >= :fromTimestamp GROUP BY s.genre",
    )
    suspend fun getGenreStats(fromTimestamp: Long): List<GenrePlayCount>

    @Query(
        "SELECT s.*, COUNT(ph.id) as totalCount FROM songs s INNER JOIN play_history ph ON s.id = ph.songId WHERE ph.playedAt >= :fromTimestamp GROUP BY s.id ORDER BY totalCount DESC LIMIT :limit",
    )
    suspend fun getTopSongs(fromTimestamp: Long, limit: Int = AppConfig.Ui.NUM_OF_TOP_SONGS): List<SongPlayCount>

    @Insert
    suspend fun insertPlayHistory(playHistory: PlayHistory)

    @Insert
    suspend fun insertPlayHistories(playHistories: List<PlayHistory>)
}
