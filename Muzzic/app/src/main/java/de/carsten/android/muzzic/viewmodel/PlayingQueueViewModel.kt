package de.carsten.android.muzzic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import de.carsten.android.muzzic.persistence.repo.PlayingQueueRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlayingQueueViewModel(
    private val repository: PlayingQueueRepository,
) : ViewModel() {
    val currentPlayingQueue: StateFlow<List<MediaItem>> =
        repository.observePlayingQueue()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

    fun clear() {
        viewModelScope.launch {
            repository.clear()
        }
    }

    fun loadPlayingQueue() {
        // No-op: handled by reactive StateFlow
    }

    fun savePlayingQueue(mediaItems: List<MediaItem>) {
        viewModelScope.launch {
            repository.addSongs(mediaItems)
        }
    }

    fun removeSongs(mediaItems: List<MediaItem>) {
        viewModelScope.launch {
            repository.removeSongs(mediaItems)
        }
    }

    fun persistCurrentQueue() {
        viewModelScope.launch {
            repository.persistQueue(currentPlayingQueue.value)
        }
    }
}
