package de.carsten.android.muzzic.viewmodel

import androidx.annotation.OptIn
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import de.carsten.android.muzzic.persistence.entity.Song
import de.carsten.android.muzzic.persistence.repo.AlbumRepository
import de.carsten.android.muzzic.service.MediaLibraryManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@OptIn(UnstableApi::class)
class AlbumSongsViewModel(
    savedStateHandle: SavedStateHandle,
    private val albumRepository: AlbumRepository,
    private val mediaLibraryManager: MediaLibraryManager,
) : ViewModel() {
    val artistName: String = checkNotNull(savedStateHandle["artistName"])
    val albumName: String = checkNotNull(savedStateHandle["albumName"])

    val songs: StateFlow<List<Song>> =
        albumRepository.getSongsByAlbum(artistName, albumName).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    fun playSong(song: Song, allSongs: List<Song>) {
        val startIndex = allSongs.indexOf(song).coerceAtLeast(0)
        mediaLibraryManager.playPlaylist(allSongs.map { it.toMediaItem() }, startIndex)
    }
}
