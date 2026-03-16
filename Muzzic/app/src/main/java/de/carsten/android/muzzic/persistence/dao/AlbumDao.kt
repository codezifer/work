package de.carsten.android.muzzic.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import de.carsten.android.muzzic.persistence.entity.ArtistAlbum
import de.carsten.android.muzzic.persistence.entity.Song
import de.carsten.android.muzzic.persistence.entity.aggregation.AlbumAggregation
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {
    @Query(
        "SELECT DISTINCT UPPER(SUBSTR(album, 1, 1)) FROM (SELECT DISTINCT album FROM songs ORDER BY album ASC) ORDER BY 1",
    )
    suspend fun getAlbumAlphabet(): List<Char>

    @Query("SELECT COUNT(*) FROM songs WHERE album < (SELECT MIN(album) FROM songs WHERE album LIKE :letter || '%')")
    suspend fun getAlbumLetterPositon(letter: String): Int

    @Query("SELECT DISTINCT album, artist, albumArt FROM songs ORDER BY album ASC")
    suspend fun getAllAlbums(): List<ArtistAlbum>

    @Query("SELECT * FROM songs WHERE album = :album AND artist = :artist ORDER BY trackNumber ASC, title ASC")
    fun getSongsByAlbum(
        album: String,
        artist: String,
    ): Flow<List<Song>>

    @Query(
        """
        SELECT
            s.album AS albumName,
            s.albumYear AS albumYear,
            s.artist AS artistName,
            COUNT(*) AS songCount,
            SUM(s.duration) AS albumDuration,
            (SELECT s2.albumArt FROM songs s2 WHERE s2.album = s.album ORDER BY s2.createdAt DESC LIMIT 1) as lastAlbumArt
        FROM songs s
        GROUP BY s.album, s.albumYear, s.artist
        ORDER BY s.album ASC
        """,
    )
    fun getAlbumAggregation(): Flow<List<AlbumAggregation>>

    @Query(
        """
        SELECT
            s.album AS albumName,
            s.albumYear AS albumYear,
            s.artist AS artistName,
            COUNT(*) AS songCount,
            SUM(s.duration) AS albumDuration,
            (SELECT s2.albumArt FROM songs s2 WHERE s2.album = s.album ORDER BY s2.createdAt DESC LIMIT 1) as lastAlbumArt
        FROM songs s
        WHERE s.artist = :artistName
        GROUP BY s.album, s.albumYear, s.artist
        ORDER BY s.albumYear DESC, s.album ASC
        """,
    )
    fun getAlbumsByArtist(artistName: String): Flow<List<AlbumAggregation>>
}
