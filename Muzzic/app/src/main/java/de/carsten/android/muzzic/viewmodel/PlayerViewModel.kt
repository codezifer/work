package de.carsten.android.muzzic.viewmodel

import android.app.Application
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import de.carsten.android.muzzic.AppConfig
import de.carsten.android.muzzic.logging.logger
import de.carsten.android.muzzic.model.MediaKeys.ALBUMS_ID
import de.carsten.android.muzzic.model.MediaKeys.ARTISTS_ID
import de.carsten.android.muzzic.persistence.repo.MusicRepository
import de.carsten.android.muzzic.service.MediaLibraryManager
import de.carsten.android.muzzic.service.VisualizerSink
import de.carsten.android.muzzic.ui.model.SongDto
import de.carsten.android.muzzic.ui.state.PlayerUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

@OptIn(ExperimentalCoroutinesApi::class, UnstableApi::class)
open class PlayerViewModel(
    private val repository: MusicRepository,
    private val mediaLibraryManager: MediaLibraryManager,
    private val visualizerSink: VisualizerSink,
    application: Application,
) : AndroidViewModel(application),
    KoinComponent {
    private val logger = this.logger()

    val browser: StateFlow<MediaBrowser?> = mediaLibraryManager.browser

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    val amplitudes: StateFlow<List<Float>> = visualizerSink.amplitudes

    val songs =
        repository.getAllSongs().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    private val playerListener =
        object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                _uiState.update { state ->
                    state.copy(
                        currentSong = mediaItem?.let { SongDto.fromMediaItem(it) },
                        duration = browser.value?.duration?.takeIf { it > 0 } ?: 0L,
                    )
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _uiState.update { it.copy(duration = browser.value?.duration?.takeIf { it > 0 } ?: 0L) }
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _uiState.update { it.copy(shuffleModeEnabled = shuffleModeEnabled) }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _uiState.update { it.copy(repeatMode = repeatMode) }
            }
        }

    init {
        scanLibrary()
        startProgressUpdater()
        setupBrowserObservation()
        setupAmplitudesObservation()
    }

    open fun setupBrowserObservation() {
        viewModelScope.launch {
            browser.collect { b ->
                if (b != null) {
                    b.addListener(playerListener)
                    _uiState.update { state ->
                        state.copy(
                            isConnected = true,
                            isPlaying = b.isPlaying,
                            currentSong = b.currentMediaItem?.let { SongDto.fromMediaItem(it) },
                            duration = b.duration.takeIf { it > 0 } ?: 0L,
                            shuffleModeEnabled = b.shuffleModeEnabled,
                            repeatMode = b.repeatMode,
                        )
                    }
                    logger.debug("MediaBrowser connected")
                }
            }
        }
    }

    private fun setupAmplitudesObservation() {
        viewModelScope.launch {
            amplitudes.collect { amps ->
                _uiState.update { it.copy(amplitudes = amps) }
            }
        }
    }

    open fun startProgressUpdater() {
        viewModelScope.launch {
            while (true) {
                val b = browser.value
                if (b != null && b.isPlaying) {
                    val pos = b.currentPosition
                    val dur = b.duration
                    _uiState.update { state ->
                        state.copy(
                            currentPosition = pos,
                            progress = if (dur > 0) pos.toFloat() / dur else 0f,
                        )
                    }
                }
                delay(AppConfig.Service.PROGRESS_DELAY_MS)
            }
        }
    }

    fun playSong(song: SongDto) {
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
        browser.value?.let { player ->
            if (player.hasPreviousMediaItem()) {
                player.seekToPreviousMediaItem()
            } else if (player.mediaItemCount > 0) {
                player.seekToDefaultPosition(player.mediaItemCount - 1)
            }
        }
    }

    fun onNextClicked() {
        browser.value?.let { player ->
            if (player.hasNextMediaItem()) {
                player.seekToNextMediaItem()
            } else if (player.mediaItemCount > 0) {
                player.seekToDefaultPosition(0)
            }
        }
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
        uiState.value.currentSong?.let { song ->
            viewModelScope.launch {
                repository.updateSongRating(song.id, rating)
            }
        }
    }

    open fun scanLibrary() {
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
}
