package de.carsten.android.muzzic.persistence.repo

import de.carsten.android.muzzic.persistence.dao.PlaylistDao
import de.carsten.android.muzzic.ui.model.PlaylistDto
import de.carsten.android.muzzic.ui.model.toDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlaylistRepository(
    val playlistDao: PlaylistDao,
) {
    fun getPlaylistInformation(): Flow<List<PlaylistDto>> = playlistDao.getPlaylistAggregation().map { it.toDto() }

    suspend fun searchPlaylists(query: String) = playlistDao.searchPlaylists(query)
}
