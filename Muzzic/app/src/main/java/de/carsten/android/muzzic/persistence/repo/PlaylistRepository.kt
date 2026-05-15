package de.carsten.android.muzzic.persistence.repo

import androidx.media3.common.MediaItem
import de.carsten.android.muzzic.persistence.dao.PlaylistDao
import de.carsten.android.muzzic.persistence.entity.Playlist
import de.carsten.android.muzzic.persistence.entity.PlaylistSong
import de.carsten.android.muzzic.ui.model.PlaylistDto
import de.carsten.android.muzzic.ui.model.toDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class PlaylistRepository(
    val playlistDao: PlaylistDao,
) {
    fun getPlaylistInformation(): Flow<List<PlaylistDto>> = playlistDao.getPlaylistAggregation().map { it.toDto() }

    suspend fun searchPlaylists(query: String) = playlistDao.searchPlaylists(query)

    suspend fun getSongsInPlaylist(playlistId: String) = playlistDao.getSongsInPlaylist(playlistId)

    suspend fun deletePlaylist(playlistId: String) = playlistDao.deletePlaylist(playlistId)

    suspend fun createPlaylistFromSongs(name: String, songs: List<MediaItem>) {
        val playlistId = UUID.randomUUID().toString()
        val playlist = Playlist(name = name).apply { id = playlistId }
        playlistDao.insertPlaylist(playlist)

        songs.forEachIndexed { index, mediaItem ->
            val songId = mediaItem.mediaMetadata.extras?.getString("songId") ?: mediaItem.mediaId
            playlistDao.insertPlaylistSong(
                PlaylistSong(
                    playlistId = playlistId,
                    songId = songId,
                    position = index
                )
            )
        }
    }
}
