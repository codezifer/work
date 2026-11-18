package de.carsten.android.muzzic.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import de.carsten.android.muzzic.persistence.entity.Song
import de.carsten.android.muzzic.persistence.entity.aggregation.AlbumAggregation
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {
    @Query("SELECT * FROM songs WHERE album = :album AND artist = :artist ORDER BY trackNumber ASC, title ASC")
    fun getSongsByAlbum(album: String, artist: String): Flow<List<Song>>

    @Query(
        """
        SELECT
            s.album AS albumName,
            MAX(s.albumYear) AS albumYear,
            s.artist AS artistName,
            COUNT(*) AS songCount,
            SUM(s.duration) AS albumDuration,
            (SELECT s2.albumArt FROM songs s2 WHERE s2.album = s.album AND s2.artist = s.artist ORDER BY s2.createdAt DESC LIMIT 1) as lastAlbumArt,
            MAX(s.createdAt) as addedAt
        FROM songs s
        WHERE s.album LIKE '%' || :query || '%'
        GROUP BY s.album, s.artist
        ORDER BY s.album ASC
        """,
    )
    suspend fun searchAlbums(query: String): List<AlbumAggregation>

    @Query(
        """
        SELECT
            s.album AS albumName,
            MAX(s.albumYear) AS albumYear,
            s.artist AS artistName,
            COUNT(*) AS songCount,
            SUM(s.duration) AS albumDuration,
            (SELECT s2.albumArt FROM songs s2 WHERE s2.album = s.album AND s2.artist = s.artist ORDER BY s2.createdAt DESC LIMIT 1) as lastAlbumArt,
            MAX(s.createdAt) as addedAt
        FROM songs_enriched s
        GROUP BY s.album, s.artist
        ORDER BY s.album COLLATE NOCASE ASC, s.sort_artist ASC
        """,
    )
    fun getAlbumAggregation(): Flow<List<AlbumAggregation>>

    /**
     * Returns the most recently added albums.
     *
     * Same result shape as [getAlbumAggregation], ordered by newest song
     * addition instead of artist, year and name.
     *
     * @param limit maximum number of albums to return.
     * @return album aggregations ordered by addition recency descending.
     */
    @Query(
        """
        SELECT
            s.album AS albumName,
            MAX(s.albumYear) AS albumYear,
            s.artist AS artistName,
            COUNT(*) AS songCount,
            SUM(s.duration) AS albumDuration,
            (SELECT s2.albumArt FROM songs s2 WHERE s2.album = s.album AND s2.artist = s.artist ORDER BY s2.createdAt DESC LIMIT 1) as lastAlbumArt,
            MAX(s.createdAt) as addedAt
        FROM songs s
        GROUP BY s.album, s.artist
        ORDER BY MAX(s.createdAt) DESC
        LIMIT :limit
        """,
    )
    suspend fun getRecentAlbums(limit: Int): List<AlbumAggregation>

    @Query(
        """
        SELECT
            s.album AS albumName,
            MAX(s.albumYear) AS albumYear,
            s.artist AS artistName,
            COUNT(*) AS songCount,
            SUM(s.duration) AS albumDuration,
            (SELECT s2.albumArt FROM songs s2 WHERE s2.album = s.album AND s2.artist = s.artist ORDER BY s2.createdAt DESC LIMIT 1) as lastAlbumArt,
            MAX(s.createdAt) as addedAt
        FROM songs s
        WHERE s.artist = :artistName
        GROUP BY s.album, s.artist
        ORDER BY albumYear DESC, s.album ASC
        """,
    )
    fun getAlbumsByArtist(artistName: String): Flow<List<AlbumAggregation>>
}
