package de.carsten.android.muzzic.persistence.repo

import de.carsten.android.muzzic.persistence.dao.ArtistDao
import de.carsten.android.muzzic.persistence.dao.GenreDao
import de.carsten.android.muzzic.ui.model.ArtistDto
import de.carsten.android.muzzic.ui.model.SongDto
import de.carsten.android.muzzic.ui.model.toDto
import de.carsten.android.muzzic.utils.GenreUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class ArtistRepository(val artistDao: ArtistDao, val genreDao: GenreDao) {
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
     * Get artists by genre, including all spelling variants.
     *
     * Both canonical names (e.g. "Death Core") and stored variants (e.g. "Deathcore")
     * resolve to the same variant group. Unknown names yield an empty list.
     *
     * @param genreName canonical genre name or stored genre variant.
     * @return A [List] of [ArtistDto]
     */
    fun getArtistsByGenre(genreName: String): Flow<List<ArtistDto>> = flow {
        val key = GenreUtils.normalizeKey(genreName)
        val variants = genreDao.getGenreCounts().map { it.genre }.filter { GenreUtils.normalizeKey(it) == key }
        if (variants.isEmpty()) {
            emit(emptyList())
        } else {
            emitAll(artistDao.getArtistAggregationsByGenres(variants).map { it.toDto() })
        }
    }
}
