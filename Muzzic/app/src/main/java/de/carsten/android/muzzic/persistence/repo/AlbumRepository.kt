package de.carsten.android.muzzic.persistence.repo

import de.carsten.android.muzzic.persistence.dao.AlbumDao
import de.carsten.android.muzzic.ui.model.AlbumDto
import de.carsten.android.muzzic.ui.model.toDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AlbumRepository(val albumDao: AlbumDao) {

    fun getAlbumInformation(): Flow<List<AlbumDto>> {
        return albumDao.getAlbumAggregation().map { it.toDto() }
    }
}
