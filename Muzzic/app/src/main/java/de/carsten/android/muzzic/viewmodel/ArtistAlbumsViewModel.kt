package de.carsten.android.muzzic.viewmodel

import androidx.annotation.OptIn
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import de.carsten.android.muzzic.persistence.repo.AlbumRepository
import de.carsten.android.muzzic.persistence.repo.PlayingQueueRepository
import de.carsten.android.muzzic.service.MediaLibraryManager
import de.carsten.android.muzzic.ui.AppDestinations.ARTIST_ARGUMENT
import de.carsten.android.muzzic.ui.state.ArtistAlbumsUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class ArtistAlbumsViewModel(
    savedStateHandle: SavedStateHandle,
    private val albumRepository: AlbumRepository,
    private val playingQueueRepository: PlayingQueueRepository,
    private val mediaLibraryManager: MediaLibraryManager,
) : AbstractViewModel(playingQueueRepository, mediaLibraryManager),
    UiStateViewModel<ArtistAlbumsUiState> {
    private val artistName: String = checkNotNull(savedStateHandle[ARTIST_ARGUMENT])

    override val uiState: StateFlow<ArtistAlbumsUiState> =
        albumRepository.getAlbumsByArtist(artistName).map { albums ->
            ArtistAlbumsUiState(
                artistName = artistName,
                albums = albums,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ArtistAlbumsUiState(artistName),
        )

    fun playAlbum(artistName: String, albumName: String) {
        viewModelScope.launch {
            enqueue(albumRepository.getSongsByAlbum(artistName, albumName).first())
        }
    }
}
