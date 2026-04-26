package de.carsten.android.muzzic.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import de.carsten.android.muzzic.persistence.entity.Song
import de.carsten.android.muzzic.persistence.entity.aggregation.ArtistAggregation
import kotlinx.coroutines.flow.Flow

@Dao
interface ArtistDao {
    @Query(
        "SELECT DISTINCT UPPER(SUBSTR(artist, 1, 1)) FROM (SELECT DISTINCT artist FROM songs ORDER BY artist ASC) ORDER BY 1",
    )
    suspend fun getArtistAlphabet(): List<Char>

    @Query("SELECT COUNT(*) FROM songs WHERE artist < (SELECT MIN(artist) FROM songs WHERE artist LIKE :letter || '%')")
    suspend fun getArtistLetterPositon(letter: String): Int

    @Query("SELECT DISTINCT artist FROM songs ORDER BY artist ASC")
    suspend fun getAllArtists(): List<String>

    @Query("SELECT * FROM songs WHERE artist = :artist ORDER BY album, title")
    suspend fun getSongsByArtist(artist: String): List<Song>

    @Query(
        """
        SELECT
            s.artist AS artistName,
            COUNT(DISTINCT s.album) AS albumCount,
            COUNT(s.id) AS songCount,
            (SELECT s2.albumArt FROM songs s2 WHERE s2.artist = s.artist ORDER BY s2.createdAt DESC LIMIT 1) as lastAlbumArt
        FROM songs s
        WHERE s.artist LIKE '%' || :query || '%'
        GROUP BY s.artist ORDER BY s.artist ASC
        """,
    )
    suspend fun searchArtists(query: String): List<ArtistAggregation>

    @Query(
        """
        SELECT
            s.artist AS artistName,
            COUNT(DISTINCT s.album) AS albumCount,
            COUNT(s.id) AS songCount,
            (SELECT s2.albumArt FROM songs s2 WHERE s2.artist = s.artist ORDER BY s2.createdAt DESC LIMIT 1) as lastAlbumArt
        FROM songs s
        GROUP BY s.artist ORDER BY s.artist ASC
        """,
    )
    fun getArtistAggregations(): Flow<List<ArtistAggregation>>
}
