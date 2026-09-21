package de.carsten.android.muzzic.viewmodel

import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import de.carsten.android.muzzic.persistence.repo.PlayingQueueRepository
import de.carsten.android.muzzic.persistence.repo.PlaylistRepository
import de.carsten.android.muzzic.service.MediaLibraryManager
import de.carsten.android.muzzic.ui.model.PlayingQueueDto
import de.carsten.android.muzzic.ui.state.PlayingQueueUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
open class PlayingQueueViewModel(
    private val repository: PlayingQueueRepository,
    private val playlistRepository: PlaylistRepository,
    private val mediaLibraryManager: MediaLibraryManager,
) : ViewModel(),
    UiStateViewModel<PlayingQueueUiState> {
    private val _uiState = MutableStateFlow(PlayingQueueUiState())
    override val uiState: StateFlow<PlayingQueueUiState> = _uiState.asStateFlow()

    private val playerListener =
        object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                _uiState.update { it.copy(currentSong = mediaItem) }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val b = mediaLibraryManager.browser.value ?: return
                _uiState.update { it.copy(duration = b.duration.takeIf { it > 0 } ?: 0L) }
            }
        }

    init {
        setupQueueObservation()
        setupBrowserObservation()
        startProgressUpdater()
    }

    open fun setupQueueObservation() {
        viewModelScope.launch {
            repository.observePlayingQueue().collect { queue ->
                _uiState.update { it.copy(queue = queue) }
            }
        }
    }

    open fun setupBrowserObservation() {
        viewModelScope.launch {
            mediaLibraryManager.browser.collect { b ->
                if (b != null) {
                    b.addListener(playerListener)
                    _uiState.update {
                        it.copy(
                            isPlaying = b.isPlaying,
                            currentSong = b.currentMediaItem,
                            duration = b.duration.takeIf { it > 0 } ?: 0L,
                        )
                    }
                }
            }
        }
    }

    open fun startProgressUpdater() {
        viewModelScope.launch {
            while (true) {
                val b = mediaLibraryManager.browser.value
                if (b != null && b.isPlaying) {
                    val pos = b.currentPosition
                    val dur = b.duration.takeIf { it > 0 } ?: 0L
                    _uiState.update { it.copy(currentPosition = pos, duration = dur) }
                }
                delay(500)
            }
        }
    }

    fun togglePlayPause() {
        val b = mediaLibraryManager.browser.value ?: return
        if (b.isPlaying) {
            b.pause()
        } else {
            b.play()
        }
    }

    fun playSongAt(index: Int) {
        val b = mediaLibraryManager.browser.value ?: return
        val currentQueue = uiState.value.queue
        if (index in currentQueue.indices) {
            // Check if the current player items match our queue
            val match =
                b.mediaItemCount == currentQueue.size &&
                    (0 until b.mediaItemCount).all { i ->
                        b.getMediaItemAt(i).mediaId == currentQueue[i].mediaId
                    }

            if (!match) {
                // If they don't match, reload the whole queue into the player
                mediaLibraryManager.playPlaylist(currentQueue.map { it.toMediaItem() }, index)
            } else {
                // If they match, just seek to the correct item
                b.seekToDefaultPosition(index)
                b.prepare()
                b.play()
            }
        }
    }

    fun saveAsPlaylist(name: String) {
        viewModelScope.launch {
            playlistRepository.createPlaylistFromSongs(name, uiState.value.queue.map { it.toMediaItem() })
        }
    }

    fun moveSong(fromIndex: Int, toIndex: Int) {
        val currentQueue = uiState.value.queue
        if (fromIndex !in currentQueue.indices || toIndex !in currentQueue.indices) return

        val playlist = currentQueue.toMutableList()
        val item = playlist.removeAt(fromIndex)
        playlist.add(toIndex, item)

        // Update player
        mediaLibraryManager.browser.value?.moveMediaItem(fromIndex, toIndex)

        // Update persistence
        viewModelScope.launch {
            repository.persistQueue(playlist.map { it.toMediaItem() })
        }
    }

    fun clear() {
        mediaLibraryManager.clearPlaylist()
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

    fun setPlayQueueName(name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(name = name) }
        }
    }

    fun shuffleQueue() {
        val shuffled = uiState.value.queue.shuffled()
        updateQueue(shuffled)
    }

    fun sortQueueByMetadata() {
        val sorted = uiState.value.queue.sortedWith(
            compareBy<PlayingQueueDto> { it.artist }
                .thenBy { it.albumYear }
                .thenBy { it.trackNumber },
        )
        updateQueue(sorted)
    }

    private fun updateQueue(newQueue: List<PlayingQueueDto>) {
        val mediaItems = newQueue.map { it.toMediaItem() }

        // Update player
        val b = mediaLibraryManager.browser.value ?: return
        b.setMediaItems(mediaItems)
        b.prepare()

        // Update persistence
        viewModelScope.launch {
            repository.persistQueue(mediaItems)
        }
    }
}
