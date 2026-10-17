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
 * The queue only stores the song reference and its queue state; read queries
 * join the `songs` table to resolve every playable value and surface the result
 * through [PlayingQueueSongAggregation].
 */
@Dao
interface PlayingQueueDao {
    /**
     * Returns every enqueued queue entry ordered by queue position, enriched with
     * the matching song's metadata.
     *
     * @return the enqueued queue with the songs attached.
     */
    @Query(
        """
        SELECT
            s.*,
            pq.enqueued AS enqueued,
            pq.queuePosition AS queuePosition
        FROM playing_queue pq
        INNER JOIN songs s ON s.id = pq.songId
        WHERE pq.enqueued = 1
        ORDER BY pq.queuePosition ASC
        """,
    )
    suspend fun findEnqueued(): List<PlayingQueueSongAggregation>

    /**
     * Observes every enqueued queue entry ordered by queue position, enriched with
     * the matching song's metadata (same shape as [findEnqueued]).
     *
     * @return a flow of the enqueued queue with the songs attached.
     */
    @Query(
        """
        SELECT
            s.*,
            pq.enqueued AS enqueued,
            pq.queuePosition AS queuePosition
        FROM playing_queue pq
        INNER JOIN songs s ON s.id = pq.songId
        WHERE pq.enqueued = 1
        ORDER BY pq.queuePosition ASC
        """,
    )
    fun observeEnqueued(): Flow<List<PlayingQueueSongAggregation>>

    /**
     * Returns the full queue, enqueued entries first, then ordered by queue
     * position, enriched with the matching song's metadata.
     *
     * @return a snapshot of the complete queue with the songs attached.
     */
    @Query(
        """
        SELECT
            s.*,
            pq.enqueued AS enqueued,
            pq.queuePosition AS queuePosition
        FROM playing_queue pq
        INNER JOIN songs s ON s.id = pq.songId
        ORDER BY
            pq.enqueued DESC,
            pq.queuePosition ASC
        """,
    )
    suspend fun findAll(): List<PlayingQueueSongAggregation>

    /**
     * Observes the full queue, enqueued entries first, then ordered by queue
     * position, enriched with the matching song's metadata (same shape as [findAll]).
     *
     * @return a flow of the complete queue.
     */
    @Query(
        """
        SELECT
            s.*,
            pq.enqueued AS enqueued,
            pq.queuePosition AS queuePosition
        FROM playing_queue pq
        INNER JOIN songs s ON s.id = pq.songId
        ORDER BY
            pq.enqueued DESC,
            pq.queuePosition ASC
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
     * Removes the queue entries referencing the given song ids.
     *
     * @param songIds song ids to delete, matched against the `songId` column.
     */
    @Query("DELETE FROM playing_queue WHERE songId IN (:songIds)")
    suspend fun removeSongs(songIds: List<String>)
}
