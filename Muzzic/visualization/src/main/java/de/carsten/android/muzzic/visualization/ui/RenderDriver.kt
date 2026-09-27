package de.carsten.android.muzzic.visualization.ui

import android.view.Choreographer
import de.carsten.android.muzzic.visualization.audio.SpectrumProcessor
import de.carsten.android.muzzic.visualization.render.FrameRenderer

/**
 * Choreographer-driven frame controller invoking [requestRender] at display rate.
 *
 * Automatically pauses frame scheduling when playback stops and bars have settled to zero (`isIdle`),
 * dropping CPU and GPU load to ~0%.
 */
class RenderDriver(val requestRender: () -> Unit, val renderer: FrameRenderer, val processor: SpectrumProcessor? = null) {
    @Volatile var isPlaying: Boolean = true
        set(value) {
            field = value
            if (value) {
                start()
            }
        }

    private var isRunning = false
    private val choreographer = Choreographer.getInstance()

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!isRunning) return

            val shouldRender = isPlaying || !renderer.isIdle
            if (shouldRender) {
                requestRender()
                choreographer.postFrameCallback(this)
            } else {
                // Bars have settled while paused: stop the loop and cut the
                // audio analysis feed so CPU/GPU drop to ~0%.
                isRunning = false
                processor?.enabled?.set(false)
            }
        }
    }

    /**
     * Starts the frame scheduling loop.
     */
    fun start() {
        processor?.enabled?.set(true)
        if (!isRunning) {
            isRunning = true
            choreographer.postFrameCallback(frameCallback)
        }
    }

    /**
     * Pauses frame scheduling and disables the audio processor.
     */
    fun stop() {
        processor?.enabled?.set(false)
        if (isRunning) {
            isRunning = false
            choreographer.removeFrameCallback(frameCallback)
        }
    }
}
