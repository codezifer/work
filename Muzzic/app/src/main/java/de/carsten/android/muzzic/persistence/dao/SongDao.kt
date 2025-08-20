package de.carsten.android.muzzic.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import de.carsten.android.muzzic.persistence.entity.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun getAllSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE genre = :genre ORDER BY rating DESC, playCount DESC LIMIT :limit")
    suspend fun getTopSongsByGenre(genre: String, limit: Int = 100): List<Song>

    @Query("SELECT DISTINCT genre FROM songs ORDER BY genre ASC")
    suspend fun getAllGenres(): List<String>

    @Query("SELECT DISTINCT artist FROM songs ORDER BY artist ASC")
    suspend fun getAllArtists(): List<String>

    @Query("SELECT * FROM songs WHERE artist = :artist ORDER BY album, title")
    suspend fun getSongsByArtist(artist: String): List<Song>

    @Query("SELECT DISTINCT album, artist FROM songs ORDER BY album ASC")
    suspend fun getAllAlbums(): List<Song>

    @Query("SELECT * FROM songs WHERE album = :album AND artist = :artist ORDER BY title")
    suspend fun getSongsByAlbum(album: String, artist: String): List<Song>

    @Query("UPDATE songs SET playCount = playCount + 1, lastPlayed = :timestamp WHERE id = :songId")
    suspend fun incrementPlayCount(songId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE songs SET rating = :rating WHERE id = :songId")
    suspend fun updateRating(songId: String, rating: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: Song)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<Song>)
}

