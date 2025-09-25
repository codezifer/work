package de.carsten.android.muzzic.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import de.carsten.android.muzzic.persistence.entity.PlayingQueue

@Dao
interface PlayingQueueDao {
    @Query("SELECT * FROM playing_queue ORDER BY queuePosition ASC")
    suspend fun findAll(): List<PlayingQueue>

    @Query("DELETE FROM playing_queue")
    suspend fun clearQueue()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongs(songs: List<PlayingQueue>)

    @Query("DELETE FROM playing_queue WHERE id IN (:songIds)")
    suspend fun removeSongs(songIds: List<String>)
}
