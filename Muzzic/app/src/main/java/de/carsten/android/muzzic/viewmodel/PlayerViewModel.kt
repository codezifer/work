package de.carsten.android.muzzic.viewmodel

import android.app.Application
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import de.carsten.android.muzzic.logging.logger
import de.carsten.android.muzzic.model.MediaKeys.ALBUMS_ID
import de.carsten.android.muzzic.model.MediaKeys.ARTISTS_ID
import de.carsten.android.muzzic.persistence.entity.Song
import de.carsten.android.muzzic.persistence.repo.MusicRepository
import de.carsten.android.muzzic.service.MediaLibraryManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

@OptIn(ExperimentalCoroutinesApi::class, UnstableApi::class)
open class PlayerViewModel(private val repository: MusicRepository, private val mediaLibraryManager: MediaLibraryManager, application: Application) :
    AndroidViewModel(application),
    KoinComponent {
    private val logger = this.logger()

    val browser: StateFlow<MediaBrowser?> = mediaLibraryManager.browser

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _shuffleModeEnabled = MutableStateFlow(false)
    val shuffleModeEnabled: StateFlow<Boolean> = _shuffleModeEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _searchResults = MutableStateFlow<List<MediaItem>>(emptyList())
    val searchResults: StateFlow<List<MediaItem>> = _searchResults.asStateFlow()

    val songs =
        repository.getAllSongs().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    private val playerListener =
        object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                _currentSong.value = mediaItem?.let { Song.fromMediaItem(it) }
                _duration.value = browser.value?.duration?.takeIf { it > 0 } ?: 0L
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _duration.value = browser.value?.duration?.takeIf { it > 0 } ?: 0L
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _shuffleModeEnabled.value = shuffleModeEnabled
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _repeatMode.value = repeatMode
            }
        }

    init {
        scanLibrary()
        startProgressUpdater()
        viewModelScope.launch {
            browser.collect { b ->
                if (b != null) {
                    _isConnected.value = true
                    b.addListener(playerListener)
                    // Initial state
                    _isPlaying.value = b.isPlaying
                    _currentSong.value = b.currentMediaItem?.let { Song.fromMediaItem(it) }
                    _duration.value = b.duration.takeIf { it > 0 } ?: 0L
                    _shuffleModeEnabled.value = b.shuffleModeEnabled
                    _repeatMode.value = b.repeatMode
                    logger.debug("MediaBrowser connected")
                }
            }
        }
    }

    private fun startProgressUpdater() {
        viewModelScope.launch {
            while (true) {
                val b = browser.value
                if (b != null && b.isPlaying) {
                    val pos = b.currentPosition
                    val dur = b.duration
                    _currentPosition.value = pos
                    if (dur > 0) {
                        _progress.value = pos.toFloat() / dur
                    }
                }
                delay(500)
            }
        }
    }

    fun playSong(song: Song) {
        mediaLibraryManager.playContent(song.toMediaItem())
    }

    fun loadPlaylist(mediaItems: List<MediaItem>) {
        mediaLibraryManager.preparePlaylist(mediaItems)
    }

    fun togglePlayPause() {
        val b = browser.value ?: return
        if (b.isPlaying) {
            b.pause()
        } else {
            b.play()
        }
    }

    fun onPrevClicked() {
        browser.value?.seekToPreviousMediaItem()
    }

    fun onNextClicked() {
        browser.value?.seekToNextMediaItem()
    }

    fun toggleShuffle() {
        val b = browser.value ?: return
        b.shuffleModeEnabled = !b.shuffleModeEnabled
    }

    fun toggleRepeatMode() {
        val b = browser.value ?: return
        val nextMode = when (b.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
            else -> Player.REPEAT_MODE_OFF
        }
        b.repeatMode = nextMode
    }

    fun onProgressChanged(progress: Float) {
        val b = browser.value ?: return
        val dur = b.duration
        if (dur > 0) {
            b.seekTo((progress * dur).toLong())
        }
    }

    fun updateRating(rating: Int) {
        _currentSong.value?.let { song ->
            viewModelScope.launch {
                repository.updateSongRating(song.id, rating)
            }
        }
    }

    fun scanLibrary() {
        viewModelScope.launch {
            repository.scanMusicLibrary()
        }
    }

    // Example of how to browse via MediaBrowser
    fun loadArtists(callback: (List<MediaItem>) -> Unit) {
        viewModelScope.launch {
            val artists = mediaLibraryManager.getChildren(ARTISTS_ID)
            callback(artists)
        }
    }

    fun loadAlbums(callback: (List<MediaItem>) -> Unit) {
        viewModelScope.launch {
            val albums = mediaLibraryManager.getChildren(ALBUMS_ID)
            callback(albums)
        }
    }

    fun search(query: String, callback: (List<MediaItem>) -> Unit) {
        val b = browser.value ?: return
        b.search(query, null)
    }

    override fun onCleared() {
        super.onCleared()
    }
}
