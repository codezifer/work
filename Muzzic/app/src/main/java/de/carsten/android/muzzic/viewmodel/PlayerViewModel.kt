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
import de.carsten.android.muzzic.persistence.repo.AppSettingsRepository
import de.carsten.android.muzzic.persistence.repo.MusicRepository
import de.carsten.android.muzzic.persistence.repo.observeVisualizerOptions
import de.carsten.android.muzzic.service.MediaLibraryManager
import de.carsten.android.muzzic.ui.model.SongDto
import de.carsten.android.muzzic.ui.state.PlayerUiState
import de.carsten.android.muzzic.ui.utils.getWmpRating
import de.carsten.android.muzzic.visualization.service.VisualizerSink
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, UnstableApi::class)
open class PlayerViewModel(
    private val musicRepository: MusicRepository,
    private val mediaLibraryManager: MediaLibraryManager,
    private val visualizerSink: VisualizerSink,
    private val appSettingsRepository: AppSettingsRepository,
    application: Application,
) : AndroidViewModel(application),
    UiStateViewModel<PlayerUiState> {
    private val logger = this.logger()

    private val browser: StateFlow<MediaBrowser?> = mediaLibraryManager.browser

    private val _uiState = MutableStateFlow(PlayerUiState())
    override val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    val spectrumBus = visualizerSink.spectrumBus
    val spectrumProcessor = visualizerSink.spectrumProcessor

    private val playerListener = object : Player.Listener {
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
            appSettingsRepository.observeVisualizerOptions().collect { options ->
                _uiState.update {
                    it.copy(
                        visualizerEngine = options.engine,
                        projectMPreset = options.preset,
                        barsShimmerEnabled = options.shimmerEnabled,
                        barsTipGlowEnabled = options.tipGlowEnabled,
                        hueRotationSpeed = options.hueRotationSpeed,
                        visualizerColorSource = options.colorSource,
                    )
                }
            }
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

    /**
     * Persists the rating of the currently playing song.
     *
     * The star value (0..5 in 0.5 steps) is stored as a WMP rating (0..255).
     * The UI state is updated optimistically because the running queue's
     * `MediaItem` keeps its original user rating and would not reflect the
     * change otherwise.
     *
     * @param stars new rating in the range 0f..5f.
     */
    fun onRatingChanged(stars: Float) {
        val song = _uiState.value.currentSong ?: return
        val wmpRating = getWmpRating(stars)
        _uiState.update { state -> state.copy(currentSong = state.currentSong?.copy(rating = wmpRating)) }
        viewModelScope.launch {
            musicRepository.updateSongRating(song.id, wmpRating)
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
            musicRepository.scanMusicLibrary()
        }
    }
}
