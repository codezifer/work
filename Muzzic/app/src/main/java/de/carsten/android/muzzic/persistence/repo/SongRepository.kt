package de.carsten.android.muzzic.persistence.repo

import de.carsten.android.muzzic.persistence.dao.SongDao

class SongRepository(private val songDao: SongDao) {
    suspend fun searchSongs(query: String) = songDao.searchSongs(query)

    suspend fun getSongById(songId: String) = songDao.getSongById(songId)

    suspend fun getSongsByIds(vararg songId: String) = songDao.getSongsByIds(*songId)
}
