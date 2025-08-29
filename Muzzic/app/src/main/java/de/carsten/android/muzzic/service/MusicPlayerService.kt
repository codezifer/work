package de.carsten.android.muzzic.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MusicPlayerService : Service() {
    private var exoPlayer: ExoPlayer? = null
    private val binder = MusicPlayerBinder()
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private val _isPlaying = MutableStateFlow(false)
    val isPlayingFlow: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentSong = MutableStateFlow<MediaItem?>(null)
    val currentSongFlow: StateFlow<MediaItem?> = _currentSong.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val durationFlow: StateFlow<Long> = _duration.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPositionFlow: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _playbackState = MutableStateFlow(Player.STATE_IDLE)
    val playbackStateFlow: StateFlow<Int> = _playbackState.asStateFlow()


    inner class MusicPlayerBinder : Binder() {
        fun getService(): MusicPlayerService = this@MusicPlayerService
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        exoPlayer = ExoPlayer.Builder(this).build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    _currentSong.value = mediaItem
                    _duration.value = exoPlayer?.duration ?: 0L
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    _playbackState.value = playbackState
                    if (playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED) {
                        _duration.value = exoPlayer?.duration ?: 0L
                    }
                }
            })
        }
        // start a coroutine to periodically update current position
        serviceScope.launch {
            while (true) {
                if (_isPlaying.value) {
                    _currentPosition.value = exoPlayer?.currentPosition ?: 0L
                }
                delay(500) // twice a second
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        serviceJob.cancel()
    }

    fun playContent(mediaItem: MediaItem) {
        exoPlayer?.let { player ->
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        }
    }

    fun pause() = exoPlayer?.pause()
    fun resume() = exoPlayer?.play()
    fun stop() = exoPlayer?.stop()

    fun isPlaying() = exoPlayer?.isPlaying ?: false
    fun getCurrentPosition() = exoPlayer?.currentPosition ?: 0L
    fun getDuration() = exoPlayer?.duration ?: 0L

    fun seekTo(position: Long) = exoPlayer?.seekTo(position)
}
