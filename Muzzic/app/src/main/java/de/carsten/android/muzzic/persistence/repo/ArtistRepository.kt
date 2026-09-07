package de.carsten.android.muzzic.persistence.repo

import de.carsten.android.muzzic.persistence.dao.ArtistDao
import de.carsten.android.muzzic.ui.model.ArtistDto
import de.carsten.android.muzzic.ui.model.SongDto
import de.carsten.android.muzzic.ui.model.toDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ArtistRepository(val artistDao: ArtistDao) {
    fun getArtistInformation(): Flow<List<ArtistDto>> = artistDao.getArtistAggregations().map { it.toDto() }

    /**
     * Retrieves all songs associated with a specific artist.
     *
     * @param artistName The name of the artist.
     * @return A [List] of [SongDto]
     */
    suspend fun getSongsByArtist(artistName: String): List<SongDto> = artistDao.getSongsByArtist(artistName).map { song -> song.toDto() }

    /**
     * Search a single artist by query from a list of artist names (SQL like search)
     *
     * @param query artist name query / pattern
     * @return [List] of [ArtistDto]
     */
    suspend fun searchArtists(query: String): List<ArtistDto> = artistDao.searchArtists(query).toDto()

    /**
     * Get artists by genre
     *
     * @param genreName genre name
     * @return A [List] of [ArtistDto]
     */
    fun getArtistsByGenre(genreName: String): Flow<List<ArtistDto>> = artistDao.getArtistAggregationsByGenre(genreName).map { it.toDto() }
}
