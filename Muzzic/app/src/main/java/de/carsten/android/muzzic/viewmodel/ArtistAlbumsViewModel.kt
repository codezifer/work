package de.carsten.android.muzzic.viewmodel

import androidx.annotation.OptIn
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import de.carsten.android.muzzic.persistence.repo.AlbumRepository
import de.carsten.android.muzzic.persistence.repo.PlayingQueueRepository
import de.carsten.android.muzzic.service.MediaLibraryManager
import de.carsten.android.muzzic.ui.AppDestinations.ARTIST_ARGUMENT
import de.carsten.android.muzzic.ui.model.AlbumDto
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class ArtistAlbumsViewModel(
    savedStateHandle: SavedStateHandle,
    private val albumRepository: AlbumRepository,
    private val playingQueueRepository: PlayingQueueRepository,
    private val mediaLibraryManager: MediaLibraryManager,
) : AbstractViewModel(playingQueueRepository, mediaLibraryManager) {
    val artistName: String = checkNotNull(savedStateHandle[ARTIST_ARGUMENT])

    val albums: StateFlow<List<AlbumDto>> =
        albumRepository.getAlbumsByArtist(artistName).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    fun playAlbum(artistName: String, albumName: String) {
        viewModelScope.launch {
            enqueue(albumRepository.getSongsByAlbum(artistName, albumName).first())
        }
    }
}
