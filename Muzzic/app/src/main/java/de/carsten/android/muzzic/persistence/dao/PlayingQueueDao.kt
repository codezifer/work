package de.carsten.android.muzzic.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import de.carsten.android.muzzic.persistence.entity.PlayingQueue
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayingQueueDao {
    @Query("SELECT * FROM playing_queue WHERE enqueued = 1 ORDER BY queuePosition ASC")
    suspend fun findEnqueued(): List<PlayingQueue>

    @Query("SELECT * FROM playing_queue WHERE enqueued = 1 ORDER BY queuePosition ASC")
    fun observeEnqueued(): Flow<List<PlayingQueue>>

    @Query("SELECT * FROM playing_queue ORDER BY enqueued DESC, queuePosition ASC")
    suspend fun findAll(): List<PlayingQueue>

    @Query("SELECT * FROM playing_queue ORDER BY enqueued DESC, queuePosition ASC")
    fun observeAll(): Flow<List<PlayingQueue>>

    @Query("DELETE FROM playing_queue")
    suspend fun clearQueue()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongs(songs: List<PlayingQueue>)

    @Query("DELETE FROM playing_queue WHERE id IN (:songIds)")
    suspend fun removeSongs(songIds: List<String>)
}
