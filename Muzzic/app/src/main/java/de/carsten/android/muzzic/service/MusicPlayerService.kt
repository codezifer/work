package de.carsten.android.muzzic.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
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

    private var currentPlaylistIndex = -1
    private var currentLastPlaylistIndex = -1

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

    private val _playlist = MutableStateFlow<List<MediaItem>>(emptyList())
    val playlistStateFlow: StateFlow<List<MediaItem>> = _playlist.asStateFlow()


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
                    // update current playlist index
                    mediaItem?.let {
                        currentPlaylistIndex =
                            _playlist.value.indexOfFirst { item -> item.mediaId == it.mediaId }
                        currentLastPlaylistIndex = _playlist.value.lastIndex
                    }
                    if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                        _currentSong.value = exoPlayer?.currentMediaItem
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    _playbackState.value = playbackState
                    if (playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED) {
                        _duration.value = exoPlayer?.duration ?: 0L
                    }
                    if (playbackState == Player.STATE_ENDED) {
                        // TODO: handle repead mode here
                    }
                }

                override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                    if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
                        // TODO: update playlist STateFlow here if needed
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

    // --- playlist management ---

    fun setPlaylist(newPlaylist: List<MediaItem>, startIndex: Int = 0) {
        _playlist.value = newPlaylist
        if (newPlaylist.isNotEmpty() && 0 <= startIndex && startIndex <= newPlaylist.size) {
            currentPlaylistIndex = startIndex
            currentLastPlaylistIndex = newPlaylist.lastIndex
            exoPlayer?.setMediaItems(newPlaylist, startIndex, 0L)
            exoPlayer?.prepare()
        } else {
            // clear player if playlist is empty or start index invalid
            exoPlayer?.clearMediaItems()
            exoPlayer?.stop()
            currentPlaylistIndex = -1
            currentLastPlaylistIndex = -1
            _currentSong.value = null
        }
    }

    fun addMediaItemToPlaylist(mediaItem: MediaItem) {
        val currentList = _playlist.value.toMutableList()
        currentList.add(mediaItem)
        _playlist.value = currentList
        exoPlayer?.addMediaItem(mediaItem)
    }

    fun playSongFromPlaylist(index: Int) {
        if (index >= 0 && index < _playlist.value.size) {
            currentPlaylistIndex = index
            currentLastPlaylistIndex = _playlist.value.lastIndex
            exoPlayer?.seekToDefaultPosition(index) // More robust way to switch within playlist
            exoPlayer?.playWhenReady = true // Ensure it plays
            exoPlayer?.prepare() // Call prepare if not already prepared or after seek
            exoPlayer?.play()
            _currentSong.value = exoPlayer?.currentMediaItem // Update current song immediately
        }
    }

    fun playContent(mediaItem: MediaItem) {
        setPlaylist(listOf(mediaItem))
        exoPlayer?.play()
    }

    // --- Playback Control Functions ---

    fun pause() = exoPlayer?.pause()
    fun play() {
        // If there's a playlist and a valid index, ensure player is ready for that item
        if (exoPlayer?.currentMediaItem == null && currentPlaylistIndex != -1 && currentPlaylistIndex < _playlist.value.size) {
            exoPlayer?.seekToDefaultPosition(currentPlaylistIndex)
            exoPlayer?.prepare()
        }
        exoPlayer?.play()
    }

    fun stop() = exoPlayer?.stop()
    fun next() {
        if (exoPlayer?.hasNextMediaItem() == true) {
            exoPlayer?.seekToNextMediaItem()
            // ExoPlayer's onMediaItemTransition will update currentPlaylistIndex and _currentSong
        } else {
            // Handle end of playlist: stop, loop, etc.
            if (exoPlayer?.repeatMode == Player.REPEAT_MODE_OFF) {
                handleSeekToDefault()
            } else if (exoPlayer?.repeatMode == Player.REPEAT_MODE_ALL) {
                exoPlayer?.seekToDefaultPosition(0)
            }
        }
    }

    fun previous() {
        if (exoPlayer?.hasPreviousMediaItem() == true) {
            exoPlayer?.seekToPreviousMediaItem()
            // ExoPlayer's onMediaItemTransition will update currentPlaylistIndex and _currentSong
        } else {
            // Handle beginning of playlist
            if (exoPlayer?.repeatMode == Player.REPEAT_MODE_OFF) {
                handleSeekToDefault()
            } else if (exoPlayer?.repeatMode == Player.REPEAT_MODE_ALL) {
                exoPlayer?.seekTo(currentLastPlaylistIndex, 0L)
            }
        }
    }

    fun changeProgress(progress: Float) {
        // TODO: convert progess 0f..1f to positionMs
    }


    fun isPlaying() = exoPlayer?.isPlaying ?: false
    fun getCurrentPosition() = exoPlayer?.currentPosition ?: 0L
    fun getDuration() = exoPlayer?.duration ?: 0L

    fun seekTo(position: Long) = exoPlayer?.seekTo(position)

    fun setRepeatMode(repeatMode: Int) { // Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ONE, Player.REPEAT_MODE_ALL
        exoPlayer?.repeatMode = repeatMode
    }

    fun setShuffleModeEnabled(shuffleModeEnabled: Boolean) {
        exoPlayer?.shuffleModeEnabled = shuffleModeEnabled
    }

    // --- private section ---
    private fun handleSeekToDefault() {
        exoPlayer?.seekToDefaultPosition(0)
        stop()
    }

}
