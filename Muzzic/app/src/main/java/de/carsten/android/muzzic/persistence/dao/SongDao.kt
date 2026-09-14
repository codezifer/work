package de.carsten.android.muzzic.persistence.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import de.carsten.android.muzzic.persistence.entity.Song
import de.carsten.android.muzzic.persistence.entity.aggregation.GenrePlayCount
import de.carsten.android.muzzic.persistence.entity.aggregation.LibrarySummary
import de.carsten.android.muzzic.persistence.entity.aggregation.MonthlyPlayCount
import de.carsten.android.muzzic.persistence.entity.aggregation.SongPlayCount
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY artist, albumYear, album, trackNumber, title")
    fun getAllSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs ORDER BY artist, albumYear, album, trackNumber, title")
    fun getAllSongsPagingSource(): PagingSource<Int, Song>

    @Query("SELECT DISTINCT UPPER(SUBSTR(title, 1, 1)) FROM songs ORDER BY 1")
    suspend fun getSongAlphabet(): List<String>

    @Query("SELECT COUNT(*) FROM songs WHERE title < (SELECT MIN(title) FROM songs WHERE title LIKE :letter || '%')")
    suspend fun getSongLetterPositon(letter: String): Int

    @Query("UPDATE songs SET playCount = playCount + 1, lastPlayed = :timestamp, updatedAt = :timestamp WHERE id = :songId")
    suspend fun incrementPlayCount(songId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE songs SET rating = :rating, updatedAt = :timestamp WHERE id = :songId")
    suspend fun updateRating(songId: String, rating: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE songs SET playCount = :playCount, updatedAt = :timestamp WHERE id = :songId")
    suspend fun updatePlayCount(songId: String, playCount: Int, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM songs WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%'")
    suspend fun searchSongs(query: String): List<Song>

    @Query("SELECT * FROM songs WHERE id = :songId")
    suspend fun getSongById(songId: String): Song?

    @Query("SELECT * FROM songs WHERE id IN (:songId)")
    suspend fun getSongsByIds(vararg songId: String): List<Song>

    @Query(
        """
        SELECT
            (SELECT COUNT(*) FROM songs) AS songCount,
            (SELECT COUNT(DISTINCT artist) FROM songs) AS artistCount,
            (SELECT COUNT(DISTINCT artist || album) FROM songs) AS albumCount,
            (SELECT COUNT(DISTINCT normalized_genre) FROM songs_enriched) AS genreCount,
            (SELECT COUNT(*) FROM playlists) AS playlistCount
        """,
    )
    suspend fun getLibrarySummary(): LibrarySummary

    @Query(
        "SELECT SUM(playCount) as count, strftime('%Y-%m', datetime(updatedAt/1000, 'unixepoch')) as month FROM songs WHERE updatedAt >= :fromTimestamp GROUP BY month ORDER BY month LIMIT :limit",
    )
    suspend fun getMonthlyStats(fromTimestamp: Long, limit: Int): List<MonthlyPlayCount>

    /**
     * Sums play counts per normalized genre key.
     *
     * Groups over [de.carsten.android.muzzic.persistence.entity.SongsEnriched], so
     * spelling variants share one row. The [genre][de.carsten.android.muzzic.persistence.entity.aggregation.GenrePlayCount.genre]
     * holds the normalized key (or [unknownLabel] for blank genres) and is mapped to
     * the canonical display name by the caller. The limit is applied after the
     * canonical merge, so variants below the cutoff still count towards their genre.
     *
     * @param fromTimestamp only songs updated after this timestamp are counted.
     * @param unknownLabel label for blank genres.
     * @return play counts per normalized genre key, ordered by count descending.
     */
    @Query(
        "SELECT CASE WHEN (s.genre IS NULL OR s.genre = '') THEN :unknownLabel ELSE s.normalized_genre END AS genre, SUM(s.playCount) AS count FROM songs_enriched s WHERE s.updatedAt >= :fromTimestamp GROUP BY 1 ORDER BY count DESC",
    )
    suspend fun getGenreStats(fromTimestamp: Long, unknownLabel: String): List<GenrePlayCount>

    @Query(
        "SELECT *, playCount as totalCount FROM songs ORDER BY rating DESC, playCount DESC LIMIT :limit",
    )
    suspend fun getTopSongs(limit: Int): List<SongPlayCount>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: Song)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<Song>)

    @Delete
    suspend fun deleteSongs(songs: List<Song>)

    @Query("DELETE FROM songs WHERE filePath = :filePath")
    suspend fun deleteSongByPath(filePath: String)

    @Query("DELETE FROM songs")
    suspend fun deleteAllSongs()
}
