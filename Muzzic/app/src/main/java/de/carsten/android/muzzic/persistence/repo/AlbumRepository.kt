package de.carsten.android.muzzic.persistence.repo

import de.carsten.android.muzzic.AppConfig
import de.carsten.android.muzzic.persistence.dao.AlbumDao
import de.carsten.android.muzzic.ui.model.AlbumDto
import de.carsten.android.muzzic.ui.model.SongDto
import de.carsten.android.muzzic.ui.model.toDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AlbumRepository(val albumDao: AlbumDao) {
    fun getAlbumInformation(): Flow<List<AlbumDto>> = albumDao.getAlbumAggregation().map { it.toDto() }

    /**
     * Returns the most recently added albums.
     *
     * @param limit maximum number of albums, defaults to [AppConfig.Ui.NUM_OF_RECENT_ITEMS].
     * @return A [List] of [AlbumDto] ordered by addition recency descending.
     */
    suspend fun getRecentAlbums(limit: Int = AppConfig.Ui.NUM_OF_RECENT_ITEMS): List<AlbumDto> = albumDao.getRecentAlbums(limit).toDto()

    fun getAlbumsByArtist(artistName: String): Flow<List<AlbumDto>> = albumDao.getAlbumsByArtist(artistName).map { it.toDto() }

    fun getSongsByAlbum(artistName: String, albumName: String): Flow<List<SongDto>> =
        albumDao.getSongsByAlbum(albumName, artistName).map { list -> list.mapIndexed { index, song -> song.toDto(index) } }

    suspend fun searchAlbums(query: String): List<AlbumDto> = albumDao.searchAlbums(query).toDto()
}
