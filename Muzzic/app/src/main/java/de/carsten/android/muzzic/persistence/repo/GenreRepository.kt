package de.carsten.android.muzzic.persistence.repo

import de.carsten.android.muzzic.persistence.dao.GenreDao
import de.carsten.android.muzzic.persistence.entity.Song
import de.carsten.android.muzzic.ui.model.GenreDto
import de.carsten.android.muzzic.ui.model.toDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GenreRepository(val genreDao: GenreDao) {
    suspend fun getSongsByGenre(genre: String): List<Song> = genreDao.getSongsByGenre(genre)
    
    fun getGenreInformation(): Flow<List<GenreDto>> = genreDao.getGenreAggregations().map { it.toDto() }
}
