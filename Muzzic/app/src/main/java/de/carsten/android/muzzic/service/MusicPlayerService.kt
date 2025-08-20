package de.carsten.android.muzzic.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

class MusicPlayerService : Service() {
    private var exoPlayer: ExoPlayer? = null
    private val binder = MusicPlayerBinder()

    inner class MusicPlayerBinder : Binder() {
        fun getService(): MusicPlayerService = this@MusicPlayerService
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        exoPlayer = ExoPlayer.Builder(this).build()
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
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
