package de.carsten.android.muzzic.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import de.carsten.android.muzzic.AppConfig
import de.carsten.android.muzzic.persistence.entity.Song
import de.carsten.android.muzzic.persistence.entity.aggregation.ArtistAggregation
import kotlinx.coroutines.flow.Flow

@Dao
interface ArtistDao {
    @Query("SELECT * FROM songs WHERE artist = :artist ORDER BY album, title")
    suspend fun getSongsByArtist(artist: String): List<Song>

    @Query(
        """
        SELECT
            s.artist AS artistName,
            COUNT(DISTINCT s.album) AS albumCount,
            COUNT(s.id) AS songCount,
            (SELECT s2.albumArt FROM songs s2 WHERE s2.artist = s.artist ORDER BY s2.createdAt DESC LIMIT 1) as lastAlbumArt,
            (SELECT GROUP_CONCAT(albumArt) FROM (
                SELECT MIN(s2.albumArt) as albumArt
                FROM songs s2
                WHERE s2.artist = s.artist AND s2.albumArt IS NOT NULL
                GROUP BY s2.album
                ORDER BY MAX(s2.createdAt) DESC LIMIT ${AppConfig.Persistence.ALBUM_ART_LIMIT}
            )) as allAlbumArts
        FROM songs s
        WHERE s.artist LIKE '%' || :query || '%'
        GROUP BY s.artist
        ORDER BY
            CASE
                WHEN s.artist LIKE 'The %' THEN substr(s.artist, 5)
                WHEN s.artist LIKE 'An %' THEN substr(s.artist, 4)
                WHEN s.artist LIKE 'A %' THEN substr(s.artist, 3)
                ELSE s.artist
            END COLLATE NOCASE
        """,
    )
    suspend fun searchArtists(query: String): List<ArtistAggregation>

    @Query(
        """
        SELECT
            s.artist AS artistName,
            COUNT(DISTINCT s.album) AS albumCount,
            COUNT(s.id) AS songCount,
            (SELECT s2.albumArt FROM songs s2 WHERE s2.artist = s.artist ORDER BY s2.createdAt DESC LIMIT 1) as lastAlbumArt,
            (SELECT GROUP_CONCAT(albumArt) FROM (
                SELECT MIN(s2.albumArt) as albumArt
                FROM songs s2
                WHERE s2.artist = s.artist AND s2.albumArt IS NOT NULL
                GROUP BY s2.album
                ORDER BY MAX(s2.createdAt) DESC LIMIT ${AppConfig.Persistence.ALBUM_ART_LIMIT}
            )) as allAlbumArts
        FROM songs s
        GROUP BY s.artist
        ORDER BY
            CASE
                WHEN s.artist LIKE 'The %' THEN substr(s.artist, 5)
                WHEN s.artist LIKE 'An %' THEN substr(s.artist, 4)
                WHEN s.artist LIKE 'A %' THEN substr(s.artist, 3)
                ELSE s.artist
            END COLLATE NOCASE
        """,
    )
    fun getArtistAggregations(): Flow<List<ArtistAggregation>>

    @Query(
        """
        SELECT
            s.artist as artistName,
            COUNT(DISTINCT s.album) as albumCount,
            COUNT(s.id) as songCount,
            (SELECT s2.albumArt FROM songs s2 WHERE s2.artist = s.artist ORDER BY s2.createdAt DESC LIMIT 1) as lastAlbumArt,
            (SELECT GROUP_CONCAT(albumArt) FROM (
                SELECT MIN(s2.albumArt) as albumArt
                FROM songs s2
                WHERE s2.artist = s.artist AND s2.albumArt IS NOT NULL
                GROUP BY s2.album
                ORDER BY MAX(s2.createdAt) DESC LIMIT ${AppConfig.Persistence.ALBUM_ART_LIMIT}
            )) as allAlbumArts
        FROM songs s
        WHERE s.genre = :genre
        GROUP BY s.artist
        ORDER BY
            CASE
                WHEN s.artist LIKE 'The %' THEN substr(s.artist, 5)
                WHEN s.artist LIKE 'An %' THEN substr(s.artist, 4)
                WHEN s.artist LIKE 'A %' THEN substr(s.artist, 3)
                ELSE s.artist
            END COLLATE NOCASE
        """,
    )
    fun getArtistAggregationsByGenre(genre: String): Flow<List<ArtistAggregation>>

    /**
     * Aggregates artists over any of the given stored genre variants.
     *
     * Used to show artists of a canonical genre across all its spelling variants.
     * The [genres] list must not be empty (an empty `IN ()` clause is invalid SQL);
     * callers return early for empty variant lists.
     *
     * @param genres stored genre variant names.
     * @return artist aggregations ordered with article-aware, case-insensitive collation.
     */
    @Query(
        """
        SELECT
            s.artist as artistName,
            COUNT(DISTINCT s.album) as albumCount,
            COUNT(s.id) as songCount,
            (SELECT s2.albumArt FROM songs s2 WHERE s2.artist = s.artist ORDER BY s2.createdAt DESC LIMIT 1) as lastAlbumArt,
            (SELECT GROUP_CONCAT(albumArt) FROM (
                SELECT MIN(s2.albumArt) as albumArt
                FROM songs s2
                WHERE s2.artist = s.artist AND s2.albumArt IS NOT NULL
                GROUP BY s2.album
                ORDER BY MAX(s2.createdAt) DESC LIMIT ${AppConfig.Persistence.ALBUM_ART_LIMIT}
            )) as allAlbumArts
        FROM songs s
        WHERE s.genre IN (:genres)
        GROUP BY s.artist
        ORDER BY
            CASE
                WHEN s.artist LIKE 'The %' THEN substr(s.artist, 5)
                WHEN s.artist LIKE 'An %' THEN substr(s.artist, 4)
                WHEN s.artist LIKE 'A %' THEN substr(s.artist, 3)
                ELSE s.artist
            END COLLATE NOCASE
        """,
    )
    fun getArtistAggregationsByGenres(genres: List<String>): Flow<List<ArtistAggregation>>
}
