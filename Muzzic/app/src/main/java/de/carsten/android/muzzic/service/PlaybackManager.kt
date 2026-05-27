package de.carsten.android.muzzic.service

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * Manages the ExoPlayer instance and provides high-level playback controls.
 */
class PlaybackManager(context: Context) {
    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()

    fun play() {
        if (exoPlayer.playbackState == Player.STATE_IDLE) {
            exoPlayer.prepare()
        }
        exoPlayer.play()
    }

    fun pause() = exoPlayer.pause()

    fun stop() = exoPlayer.stop()

    fun next() {
        if (exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNextMediaItem()
        } else if (exoPlayer.repeatMode == Player.REPEAT_MODE_ALL) {
            exoPlayer.seekToDefaultPosition(0)
        } else {
            exoPlayer.seekToDefaultPosition(0)
            stop()
        }
    }

    fun previous() {
        if (exoPlayer.hasPreviousMediaItem()) {
            exoPlayer.seekToPreviousMediaItem()
        } else if (exoPlayer.repeatMode == Player.REPEAT_MODE_ALL) {
            exoPlayer.seekTo(exoPlayer.mediaItemCount - 1, 0L)
        } else {
            exoPlayer.seekToDefaultPosition(0)
            stop()
        }
    }

    fun seekTo(position: Long) = exoPlayer.seekTo(position)

    fun setRepeatMode(repeatMode: Int) {
        exoPlayer.repeatMode = repeatMode
    }

    fun setShuffleModeEnabled(enabled: Boolean) {
        exoPlayer.shuffleModeEnabled = enabled
    }

    fun release() {
        exoPlayer.release()
    }
}
