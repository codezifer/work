package de.carsten.android.muzzic.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.carsten.android.muzzic.persistence.repo.AlbumRepository
import de.carsten.android.muzzic.ui.model.AlbumDto
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ArtistAlbumsViewModel(
    savedStateHandle: SavedStateHandle,
    albumRepository: AlbumRepository,
) : ViewModel() {
    val artistName: String = checkNotNull(savedStateHandle["artistName"])

    val albums: StateFlow<List<AlbumDto>> =
        albumRepository.getAlbumsByArtist(artistName).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )
}
