package de.carsten.android.muzzic.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import de.carsten.android.muzzic.AppConfig
import de.carsten.android.muzzic.persistence.entity.Song
import de.carsten.android.muzzic.persistence.entity.aggregation.CanonicalGenreAggregation
import de.carsten.android.muzzic.persistence.entity.aggregation.GenreAggregation
import de.carsten.android.muzzic.persistence.entity.aggregation.GenrePlayCount
import kotlinx.coroutines.flow.Flow

@Dao
interface GenreDao {

    /**
     * Returns songs of any of the given stored genre variants.
     *
     * Used to resolve a canonical genre name to songs of all its spelling variants.
     * The [genres] list must not be empty (an empty `IN ()` clause is invalid SQL);
     * callers return early for empty variant lists.
     *
     * @param genres stored genre variant names.
     * @return songs ordered by artist, year, album and track number.
     */
    @Query("SELECT * FROM songs WHERE genre IN (:genres) ORDER BY artist ASC, albumYear DESC, album ASC, trackNumber ASC")
    suspend fun getSongsByGenres(genres: List<String>): List<Song>

    @Query("SELECT * FROM songs WHERE genre = :genre ORDER BY rating DESC, playCount DESC LIMIT :limit")
    suspend fun getTopSongsByGenre(genre: String, limit: Int = AppConfig.Persistence.NUM_TOP_SONGS): List<Song>

    /**
     * Counts songs per stored genre variant.
     *
     * Used to group spelling variants (e.g. "Death Core", "Death-Core", "Deathcore")
     * into one canonical auto-playlist via [de.carsten.android.muzzic.utils.GenreUtils].
     *
     * @return one entry per distinct stored genre string with its song count.
     */
    @Query("SELECT genre AS genre, COUNT(*) AS count FROM songs GROUP BY genre")
    suspend fun getGenreCounts(): List<GenrePlayCount>

    @Query(
        """
        SELECT
            s.genre AS genreName,
            COUNT(DISTINCT s.artist) AS artistCount,
            COUNT(DISTINCT s.album) AS albumCount,
            COUNT(*) AS songCount,
            SUM(s.duration) AS genreDuration,
            (SELECT s2.albumArt FROM songs s2 WHERE s2.genre = s.genre ORDER BY s2.createdAt DESC LIMIT 1) as lastAlbumArt,
            (SELECT GROUP_CONCAT(albumArt) FROM (SELECT MIN(s2.albumArt) as albumArt FROM songs s2 WHERE s2.genre = s.genre AND s2.albumArt IS NOT NULL GROUP BY s2.artist, s2.album ORDER BY MAX(s2.createdAt) DESC LIMIT ${AppConfig.Persistence.ALBUM_ART_LIMIT})) as allAlbumArts
        FROM songs s
        GROUP BY s.genre
        ORDER BY s.genre ASC
        """,
    )
    fun getGenreAggregations(): Flow<List<GenreAggregation>>

    /**
     * Aggregates song statistics by normalized genre key.
     *
     * The normalization (lowercase, spaces and hyphens removed) mirrors
     * [de.carsten.android.muzzic.utils.GenreUtils.normalizeKey], so spelling variants
     * like "Death Core", "Death-Core" and "Deathcore" share one row with exact
     * distinct artist/album counts and summed song counts and durations.
     *
     * @return one entry per normalized genre key, ordered by key.
     */
    @Query(
        """
        SELECT
            LOWER(REPLACE(REPLACE(s.genre, ' ', ''), '-', '')) AS normalizedKey,
            COUNT(DISTINCT s.artist) AS artistCount,
            COUNT(DISTINCT s.album) AS albumCount,
            COUNT(*) AS songCount,
            SUM(s.duration) AS genreDuration,
            (SELECT s2.albumArt FROM songs s2 WHERE LOWER(REPLACE(REPLACE(s2.genre, ' ', ''), '-', '')) = LOWER(REPLACE(REPLACE(s.genre, ' ', ''), '-', '')) ORDER BY s2.createdAt DESC LIMIT 1) as lastAlbumArt,
            (SELECT GROUP_CONCAT(albumArt) FROM (SELECT MIN(s2.albumArt) as albumArt FROM songs s2 WHERE LOWER(REPLACE(REPLACE(s2.genre, ' ', ''), '-', '')) = LOWER(REPLACE(REPLACE(s.genre, ' ', ''), '-', '')) AND s2.albumArt IS NOT NULL GROUP BY s2.artist, s2.album ORDER BY MAX(s2.createdAt) DESC LIMIT ${AppConfig.Persistence.ALBUM_ART_LIMIT})) as allAlbumArts
        FROM songs s
        GROUP BY LOWER(REPLACE(REPLACE(s.genre, ' ', ''), '-', ''))
        ORDER BY normalizedKey ASC
        """,
    )
    fun getCanonicalGenreAggregations(): Flow<List<CanonicalGenreAggregation>>
}
