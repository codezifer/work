package de.carsten.android.muzzic.persistence.repo

import androidx.media3.common.MediaItem
import de.carsten.android.muzzic.mediaId
import de.carsten.android.muzzic.model.MediaKeys
import de.carsten.android.muzzic.persistence.dao.PlaylistDao
import de.carsten.android.muzzic.persistence.entity.Playlist
import de.carsten.android.muzzic.persistence.entity.PlaylistSong
import de.carsten.android.muzzic.ui.model.PlaylistDto
import de.carsten.android.muzzic.ui.model.SongDto
import de.carsten.android.muzzic.ui.model.toDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlaylistRepository(val playlistDao: PlaylistDao) {
    fun getPlaylistInformation(): Flow<List<PlaylistDto>> = playlistDao.getPlaylistAggregation().map { it.toDto() }

    suspend fun searchPlaylists(query: String): List<Playlist> = playlistDao.searchPlaylists(query)

    suspend fun getSongsInPlaylist(playlistId: String): List<SongDto> = playlistDao.getSongsInPlaylist(playlistId).map { song -> song.toDto() }

    suspend fun deletePlaylist(playlistId: String): Unit = playlistDao.deletePlaylist(playlistId)

    suspend fun createPlaylistFromSongs(name: String, songs: List<MediaItem>) {
        val playlistId = mediaId(name).toString()
        val playlist = Playlist(name).apply { id = playlistId }
        playlistDao.insertPlaylist(playlist)

        songs.forEachIndexed { index, mediaItem ->
            val songId = mediaItem.mediaMetadata.extras?.getString(MediaKeys.SONG_ID) ?: mediaItem.mediaId
            playlistDao.insertPlaylistSong(
                PlaylistSong(
                    playlistId = playlistId,
                    songId = songId,
                    position = index,
                ),
            )
        }
    }
}
