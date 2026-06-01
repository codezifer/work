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
import de.carsten.android.muzzic.ui.PLAYING_QUEUE
import de.carsten.android.muzzic.ui.model.PlayingQueueDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class PlayingQueueViewModel(
    private val repository: PlayingQueueRepository,
    private val playlistRepository: PlaylistRepository,
    private val mediaLibraryManager: MediaLibraryManager,
) : ViewModel() {
    val currentPlayingQueue: StateFlow<List<PlayingQueueDto>> =
        repository.observePlayingQueue().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    private val _currentSong = MutableStateFlow<MediaItem?>(null)
    val currentSong: StateFlow<MediaItem?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _currentName = MutableStateFlow(PLAYING_QUEUE)
    val currentName = _currentName.asStateFlow()

    private val playerListener =
        object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                _currentSong.value = mediaItem
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val b = mediaLibraryManager.browser.value ?: return
                _duration.value = b.duration.takeIf { it > 0 } ?: 0L
            }
        }

    init {
        viewModelScope.launch {
            mediaLibraryManager.browser.collect { b ->
                if (b != null) {
                    b.addListener(playerListener)
                    _isPlaying.value = b.isPlaying
                    _currentSong.value = b.currentMediaItem
                    _duration.value = b.duration.takeIf { it > 0 } ?: 0L
                }
            }
        }
        startProgressUpdater()
    }

    private fun startProgressUpdater() {
        viewModelScope.launch {
            while (true) {
                val b = mediaLibraryManager.browser.value
                if (b != null && b.isPlaying) {
                    _currentPosition.value = b.currentPosition
                    _duration.value = b.duration.takeIf { it > 0 } ?: 0L
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
        if (index in currentPlayingQueue.value.indices) {
            // Check if the current player items match our queue
            val match =
                b.mediaItemCount == currentPlayingQueue.value.size &&
                    (0 until b.mediaItemCount).all { i ->
                        b.getMediaItemAt(i).mediaId == currentPlayingQueue.value[i].mediaId
                    }

            if (!match) {
                // If they don't match, reload the whole queue into the player
                mediaLibraryManager.playPlaylist(currentPlayingQueue.value.map { it.toMediaItem() }, index)
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
            playlistRepository.createPlaylistFromSongs(name, currentPlayingQueue.value.map { it.toMediaItem() })
        }
    }

    fun moveSong(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in currentPlayingQueue.value.indices || toIndex !in currentPlayingQueue.value.indices) return

        val playlist = currentPlayingQueue.value.toMutableList()
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
            _currentName.value = name
        }
    }
}
