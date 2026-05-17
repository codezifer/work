package de.carsten.android.muzzic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.carsten.android.muzzic.persistence.repo.PlayingQueueRepository
import de.carsten.android.muzzic.persistence.repo.PlaylistRepository
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class PlaylistViewModel(
    val repository: PlaylistRepository,
    val playingQueueRepository: PlayingQueueRepository,
) : ViewModel(), KoinComponent {

    fun persistCurrentQueueAsPlaylist(name: String) {
        viewModelScope.launch {
            repository.createPlaylistFromSongs(name, playingQueueRepository.getPlayingQueue())
        }
    }
}
