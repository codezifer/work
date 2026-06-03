package de.carsten.android.muzzic.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import de.carsten.android.muzzic.AppConfig
import de.carsten.android.muzzic.persistence.entity.Song
import de.carsten.android.muzzic.persistence.entity.aggregation.GenreAggregation
import kotlinx.coroutines.flow.Flow

@Dao
interface GenreDao {
    @Query("SELECT * FROM songs WHERE genre = :genre ORDER BY rating DESC, playCount DESC LIMIT :limit")
    suspend fun getTopSongsByGenre(genre: String, limit: Int = 100): List<Song>

    @Query("SELECT DISTINCT genre FROM songs ORDER BY genre ASC")
    suspend fun getAllGenres(): List<String>

    @Query(
        """
        SELECT
            s.genre AS genreName,
            COUNT(DISTINCT s.artist) AS artistCount,
            COUNT(DISTINCT s.album) AS albumCount,
            COUNT(*) AS songCount,
            SUM(s.duration) AS genreDuration,
            (SELECT s2.albumArt FROM songs s2 WHERE s2.genre = s.genre ORDER BY s2.createdAt DESC LIMIT 1) as lastAlbumArt,
            (SELECT GROUP_CONCAT(albumArt) FROM (SELECT DISTINCT s2.albumArt FROM songs s2 WHERE s2.genre = s.genre AND s2.albumArt IS NOT NULL ORDER BY s2.createdAt DESC LIMIT ${AppConfig.Persistence.ALBUM_ART_LIMIT})) as allAlbumArts
        FROM songs s
        GROUP BY s.genre
        ORDER BY s.genre ASC
        """,
    )
    fun getGenreAggregations(): Flow<List<GenreAggregation>>
}
