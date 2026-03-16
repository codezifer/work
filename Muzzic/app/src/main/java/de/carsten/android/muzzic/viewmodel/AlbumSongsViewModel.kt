package de.carsten.android.muzzic.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.carsten.android.muzzic.persistence.entity.Song
import de.carsten.android.muzzic.persistence.repo.AlbumRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class AlbumSongsViewModel(
    savedStateHandle: SavedStateHandle,
    albumRepository: AlbumRepository,
) : ViewModel() {
    val artistName: String = checkNotNull(savedStateHandle["artistName"])
    val albumName: String = checkNotNull(savedStateHandle["albumName"])

    val songs: StateFlow<List<Song>> =
        albumRepository.getSongsByAlbum(artistName, albumName).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )
}
