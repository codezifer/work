package de.carsten.android.muzzic.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import de.carsten.android.muzzic.persistence.repo.PlayingQueueRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PlayingQueueViewModel(private val repository: PlayingQueueRepository) : ViewModel() {

    private val _currentPlayingQueue = MutableStateFlow<List<MediaItem>>(emptyList())
    val currentPlayingQueue: StateFlow<List<MediaItem>> = _currentPlayingQueue

    fun clear() {
        viewModelScope.launch {
            repository.clear()
        }
    }

    fun loadPlayingQueue() {
        viewModelScope.launch {
            _currentPlayingQueue.value = repository.getPlayingQueue()
        }
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
}
