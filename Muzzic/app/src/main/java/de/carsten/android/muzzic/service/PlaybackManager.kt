package de.carsten.android.muzzic.service

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import de.carsten.android.muzzic.visualization.service.VisualizerSink

/**
 * Manages the ExoPlayer instance and provides high-level playback controls.
 */
@OptIn(UnstableApi::class)
class PlaybackManager(context: Context, visualizerSink: VisualizerSink) {
    private val teeAudioProcessor = TeeAudioProcessor(visualizerSink)

    private val renderersFactory =
        object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(context: Context, enableFloatOutput: Boolean, enableAudioOutputPlaybackParams: Boolean): AudioSink = DefaultAudioSink.Builder(context)
                .setAudioProcessors(arrayOf(teeAudioProcessor))
                .build()
        }

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context, renderersFactory).build()

    fun release() {
        exoPlayer.stop()
        exoPlayer.release()
    }
}
