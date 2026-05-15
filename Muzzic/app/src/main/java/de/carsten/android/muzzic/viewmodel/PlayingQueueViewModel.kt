package de.carsten.android.muzzic.viewmodel

import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import de.carsten.android.muzzic.persistence.repo.PlayingQueueRepository
import de.carsten.android.muzzic.persistence.repo.PlaylistRepository
import de.carsten.android.muzzic.service.MediaLibraryManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class PlayingQueueViewModel(
    private val repository: PlayingQueueRepository,
    private val playlistRepository: PlaylistRepository,
    private val mediaLibraryManager: MediaLibraryManager,
) : ViewModel() {
    private val _currentPlayingQueue = MutableStateFlow<List<MediaItem>>(emptyList())
    val currentPlayingQueue: StateFlow<List<MediaItem>> = _currentPlayingQueue.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observePlayingQueue().collect {
                _currentPlayingQueue.value = it
            }
        }
    }

    fun saveAsPlaylist(name: String) {
        viewModelScope.launch {
            playlistRepository.createPlaylistFromSongs(name, _currentPlayingQueue.value)
        }
    }

    fun moveSong(fromIndex: Int, toIndex: Int) {
        val currentList = _currentPlayingQueue.value.toMutableList()
        if (fromIndex !in currentList.indices || toIndex !in currentList.indices) return

        val item = currentList.removeAt(fromIndex)
        currentList.add(toIndex, item)
        _currentPlayingQueue.value = currentList

        // Update player
        mediaLibraryManager.browser.value?.moveMediaItem(fromIndex, toIndex)

        // Update persistence
        viewModelScope.launch {
            repository.persistQueue(currentList)
        }
    }

    fun clear() {
        viewModelScope.launch {
            repository.clear()
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

    fun persistCurrentQueue() {
        viewModelScope.launch {
            repository.persistQueue(repository.getCompleteQueue())
        }
    }
}
