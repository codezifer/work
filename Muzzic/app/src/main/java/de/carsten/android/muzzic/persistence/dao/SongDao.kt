package de.carsten.android.muzzic.persistence.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import de.carsten.android.muzzic.persistence.entity.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY artist ASC")
    fun getAllSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs ORDER BY artist ASC")
    fun getAllSongsPagingSource(): PagingSource<Int, Song>

    @Query("SELECT DISTINCT UPPER(SUBSTR(title, 1, 1)) FROM songs ORDER BY 1")
    suspend fun getSongAlphabet(): List<Char>

    @Query("SELECT COUNT(*) FROM songs WHERE title < (SELECT MIN(title) FROM songs WHERE title LIKE :letter || '%')")
    suspend fun getSongLetterPositon(letter: String): Int

    @Query("UPDATE songs SET playCount = playCount + 1, lastPlayed = :timestamp WHERE id = :songId")
    suspend fun incrementPlayCount(
        songId: String,
        timestamp: Long = System.currentTimeMillis(),
    )

    @Query("UPDATE songs SET rating = :rating WHERE id = :songId")
    suspend fun updateRating(
        songId: String,
        rating: Int,
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: Song)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<Song>)
}
