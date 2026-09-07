package de.carsten.android.muzzic.persistence.repo

import de.carsten.android.muzzic.persistence.dao.GenreDao
import de.carsten.android.muzzic.ui.model.GenreDto
import de.carsten.android.muzzic.ui.model.SongDto
import de.carsten.android.muzzic.ui.model.toDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GenreRepository(val genreDao: GenreDao) {
    suspend fun getSongsByGenre(genre: String): List<SongDto> = genreDao.getSongsByGenre(genre).map { song -> song.toDto() }

    fun getGenreInformation(): Flow<List<GenreDto>> = genreDao.getGenreAggregations().map { it.toDto() }
}
