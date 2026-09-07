package de.carsten.android.muzzic.persistence.repo

import de.carsten.android.muzzic.persistence.dao.SongDao
import de.carsten.android.muzzic.ui.model.SongDto

class SongRepository(private val songDao: SongDao) {
    suspend fun searchSongs(query: String): List<SongDto> = songDao.searchSongs(query).map { song -> song.toDto() }

    suspend fun getSongById(songId: String): SongDto? = songDao.getSongById(songId)?.toDto()

    suspend fun getSongsByIds(vararg songId: String): List<SongDto> = songDao.getSongsByIds(*songId).map { song -> song.toDto() }
}
