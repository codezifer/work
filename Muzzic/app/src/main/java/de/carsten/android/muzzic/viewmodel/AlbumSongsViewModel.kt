package de.carsten.android.muzzic.viewmodel

import androidx.annotation.OptIn
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import de.carsten.android.muzzic.persistence.repo.AlbumRepository
import de.carsten.android.muzzic.service.MediaLibraryManager
import de.carsten.android.muzzic.ui.model.SongDto
import de.carsten.android.muzzic.ui.state.AlbumSongsUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(UnstableApi::class)
class AlbumSongsViewModel(savedStateHandle: SavedStateHandle, private val albumRepository: AlbumRepository, private val mediaLibraryManager: MediaLibraryManager) : ViewModel() {
    private val artistName: String = checkNotNull(savedStateHandle["artistName"])
    private val albumName: String = checkNotNull(savedStateHandle["albumName"])

    val uiState: StateFlow<AlbumSongsUiState> =
        albumRepository.getSongsByAlbum(artistName, albumName).map { songs ->
            AlbumSongsUiState(
                artistName = artistName,
                albumName = albumName,
                songs = songs,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AlbumSongsUiState(artistName, albumName),
        )

    fun playSong(song: SongDto, allSongs: List<SongDto>) {
        val startIndex = allSongs.indexOf(song).coerceAtLeast(0)
        mediaLibraryManager.playPlaylist(allSongs.map { it.toMediaItem() }, startIndex)
    }
}
