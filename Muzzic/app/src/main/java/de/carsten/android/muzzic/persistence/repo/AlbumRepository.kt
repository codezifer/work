package de.carsten.android.muzzic.persistence.repo

import de.carsten.android.muzzic.persistence.dao.AlbumDao
import de.carsten.android.muzzic.ui.model.AlbumDto
import de.carsten.android.muzzic.ui.model.SongDto
import de.carsten.android.muzzic.ui.model.toDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AlbumRepository(val albumDao: AlbumDao) {
    fun getAlbumInformation(): Flow<List<AlbumDto>> = albumDao.getAlbumAggregation().map { it.toDto() }

    fun getAlbumsByArtist(artistName: String): Flow<List<AlbumDto>> = albumDao.getAlbumsByArtist(artistName).map { it.toDto() }

    fun getSongsByAlbum(artistName: String, albumName: String): Flow<List<SongDto>> =
        albumDao.getSongsByAlbum(albumName, artistName).map { list -> list.map { song -> song.toDto() } }

    suspend fun searchAlbums(query: String): List<AlbumDto> = albumDao.searchAlbums(query).toDto()
}
