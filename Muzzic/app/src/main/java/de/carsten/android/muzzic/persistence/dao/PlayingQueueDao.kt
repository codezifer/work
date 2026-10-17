package de.carsten.android.muzzic.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import de.carsten.android.muzzic.persistence.entity.PlayingQueue
import de.carsten.android.muzzic.persistence.entity.aggregation.PlayingQueueSongAggregation
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for the [PlayingQueue] table.
 *
 * Read queries that need song-level values (rating, play count) join the `songs`
 * table and surface the result through [PlayingQueueSongAggregation].
 */
@Dao
interface PlayingQueueDao {
    /**
     * Returns every enqueued queue entry ordered by queue position, enriched with
     * the matching song's `playCount` and `rating`.
     *
     * The join is a `LEFT JOIN`, so an entry whose song was deleted is still
     * returned; its `playCount` and `rating` then fall back to their defaults.
     *
     * @return the enqueued queue with song-level values attached.
     */
    @Query(
        """
        SELECT
            pq.*,
            s.playCount AS playCount,
            s.rating AS rating
        FROM playing_queue pq
        LEFT JOIN songs s ON pq.songId = s.id
        WHERE pq.enqueued = 1
        ORDER BY pq.queuePosition ASC
        """,
    )
    suspend fun findEnqueued(): List<PlayingQueueSongAggregation>

    /**
     * Observes every enqueued queue entry ordered by queue position, enriched with
     * the matching song's `playCount` and `rating` (same shape as [findEnqueued]).
     *
     * @return a flow of the enqueued queue with song-level values attached.
     */
    @Query(
        """
        SELECT
            pq.*,
            s.playCount AS playCount,
            s.rating AS rating
        FROM playing_queue pq
        LEFT JOIN songs s ON pq.songId = s.id
        WHERE pq.enqueued = 1
        ORDER BY pq.queuePosition ASC
        """,
    )
    fun observeEnqueued(): Flow<List<PlayingQueueSongAggregation>>

    /**
     * Returns the full queue, enqueued entries first, then ordered by queue position,
     * enriched with the matching song's `playCount` and `rating`.
     *
     * The join is a `LEFT JOIN`, so an entry whose song was deleted is still
     * returned; its `playCount` and `rating` then fall back to their defaults.
     *
     * @return a snapshot of the complete queue with song-level values attached.
     */
    @Query(
        """
        SELECT
            pq.*,
            s.playCount AS playCount,
            s.rating AS rating
        FROM playing_queue pq
        LEFT JOIN songs s ON pq.songId = s.id
        ORDER BY
            enqueued DESC,
            queuePosition ASC
        """,
    )
    suspend fun findAll(): List<PlayingQueueSongAggregation>

    /**
     * Observes the full queue, enqueued entries first, then ordered by queue position.
     *
     * Unlike [findAll] this emits a plain [PlayingQueue], so the joined song columns
     * are not mapped onto the result.
     *
     * @return a flow of the complete queue.
     */
    @Query(
        """
        SELECT
            pq.*,
            s.playCount AS playCount,
            s.rating AS rating
        FROM playing_queue pq
        LEFT JOIN songs s ON pq.songId = s.id
        ORDER BY
            enqueued DESC,
            queuePosition ASC
        """,
    )
    fun observeAll(): Flow<List<PlayingQueueSongAggregation>>

    /**
     * Removes every entry from the queue.
     */
    @Query("DELETE FROM playing_queue")
    suspend fun clearQueue()

    /**
     * Inserts or replaces the given queue entries.
     *
     * @param songs entries to persist; rows with an existing id are replaced.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongs(songs: List<PlayingQueue>)

    /**
     * Removes the queue entries matching the given ids.
     *
     * @param songIds media ids to delete, matched against the `id` column.
     */
    @Query("DELETE FROM playing_queue WHERE id IN (:songIds)")
    suspend fun removeSongs(songIds: List<String>)
}
