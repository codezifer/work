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
import de.carsten.android.muzzic.persistence.repo.AppSettingsRepository
import de.carsten.android.muzzic.persistence.repo.MusicRepository
import de.carsten.android.muzzic.service.MediaLibraryManager
import de.carsten.android.muzzic.ui.model.SongDto
import de.carsten.android.muzzic.ui.state.PlayerUiState
import de.carsten.android.muzzic.visualization.service.VisualizerSink
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, UnstableApi::class)
open class PlayerViewModel(
    private val repository: MusicRepository,
    private val mediaLibraryManager: MediaLibraryManager,
    private val visualizerSink: VisualizerSink,
    private val appSettingsRepository: AppSettingsRepository,
    application: Application,
) : AndroidViewModel(application),
    UiStateViewModel<PlayerUiState> {
    private val logger = this.logger()

    val browser: StateFlow<MediaBrowser?> = mediaLibraryManager.browser

    private val _uiState = MutableStateFlow(PlayerUiState())
    override val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    val spectrumBus = visualizerSink.spectrumBus
    val spectrumProcessor = visualizerSink.spectrumProcessor

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
        setupSettingsObservation()
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

    private fun setupSettingsObservation() {
        viewModelScope.launch {
            combine(
                appSettingsRepository.observeVisualizerEngine(),
                appSettingsRepository.observeProjectMPreset(),
                appSettingsRepository.observeBarsShimmerEnabled(),
                appSettingsRepository.observeBarsTipGlowEnabled(),
            ) { engine, preset, shimmerEnabled, tipGlowEnabled ->
                _uiState.update {
                    it.copy(
                        visualizerEngine = engine,
                        projectMPreset = preset,
                        barsShimmerEnabled = shimmerEnabled,
                        barsTipGlowEnabled = tipGlowEnabled,
                    )
                }
            }.collect {}
        }
    }

    fun togglePlayPause() {
        val b = browser.value ?: return
        if (b.isPlaying) {
            b.pause()
        } else {
            b.play()
        }
    }

    fun onNextClicked() {
        browser.value?.seekToNextMediaItem()
    }

    fun onPrevClicked() {
        browser.value?.seekToPreviousMediaItem()
    }

    fun onProgressChanged(progress: Float) {
        val b = browser.value ?: return
        val targetPosition = (b.duration * progress).toLong()
        b.seekTo(targetPosition)
        _uiState.update {
            it.copy(
                progress = progress,
                currentPosition = targetPosition,
            )
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
            else -> Player.REPEAT_MODE_OFF
        }
        b.repeatMode = nextMode
    }

    fun playSong(songId: String) {
        val b = browser.value ?: return
        val targetIndex = (0 until b.mediaItemCount).firstOrNull { i ->
            b.getMediaItemAt(i).mediaId == songId
        }

        if (targetIndex != null) {
            b.seekTo(targetIndex, 0L)
            b.prepare()
            b.play()
        }
    }

    fun playArtist(artistName: String) {
        val b = browser.value ?: return
        viewModelScope.launch {
            val children = mediaLibraryManager.getChildren("$ARTISTS_ID/$artistName")
            if (children.isNotEmpty()) {
                b.setMediaItems(children)
                b.prepare()
                b.play()
            }
        }
    }

    fun playAlbum(albumTitle: String) {
        val b = browser.value ?: return
        viewModelScope.launch {
            val children = mediaLibraryManager.getChildren("$ALBUMS_ID/$albumTitle")
            if (children.isNotEmpty()) {
                b.setMediaItems(children)
                b.prepare()
                b.play()
            }
        }
    }

    open fun startProgressUpdater() {
        viewModelScope.launch {
            while (true) {
                val b = browser.value
                if (b != null && b.isPlaying && b.duration > 0) {
                    val pos = b.currentPosition
                    val dur = b.duration
                    _uiState.update {
                        it.copy(
                            currentPosition = pos,
                            duration = dur,
                            progress = pos.toFloat() / dur.toFloat(),
                        )
                    }
                }
                delay(AppConfig.Service.PROGRESS_DELAY_MS)
            }
        }
    }

    open fun scanLibrary() {
        viewModelScope.launch {
            repository.scanMusicLibrary()
        }
    }
}
