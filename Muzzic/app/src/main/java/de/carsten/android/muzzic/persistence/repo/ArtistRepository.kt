package de.carsten.android.muzzic.persistence.repo

import de.carsten.android.muzzic.persistence.dao.ArtistDao
import de.carsten.android.muzzic.ui.model.ArtistDto
import de.carsten.android.muzzic.ui.model.toDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ArtistRepository(
    val artistDao: ArtistDao,
) {
    fun getArtistInformation(): Flow<List<ArtistDto>> = artistDao.getArtistAggregations().map { it.toDto() }

    suspend fun searchArtists(query: String): List<ArtistDto> = artistDao.searchArtists(query).toDto()
}
