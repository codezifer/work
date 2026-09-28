package de.carsten.android.muzzic.visualization.ui

import android.view.Choreographer
import de.carsten.android.muzzic.visualization.NANOS_PER_SECOND
import de.carsten.android.muzzic.visualization.audio.SpectrumProcessor
import de.carsten.android.muzzic.visualization.render.FrameRenderer

/**
 * Choreographer-driven frame controller invoking [requestRender] at display rate.
 *
 * Automatically pauses frame scheduling when playback stops and bars have settled to zero (`isIdle`),
 * dropping CPU and GPU load to ~0%.
 *
 * The driver must be created on the thread whose frames it drives (the render thread):
 * [Choreographer.getInstance] binds to the calling thread's looper. [start] and [stop]
 * are thread-safe and may be called from any thread, but must target a living looper.
 *
 * @param maxFps Optional frame-rate cap (e.g. 30 for visualizers); `null` renders at
 * native display refresh rate.
 */
class RenderDriver(val requestRender: () -> Unit, val renderer: FrameRenderer, val processor: SpectrumProcessor? = null, @Volatile var maxFps: Int? = null) {
    @Volatile var isPlaying: Boolean = true
        set(value) {
            field = value
            if (value) {
                start()
            }
        }

    @Volatile private var isRunning = false

    @Volatile private var lastRenderNanos: Long = 0L
    private val choreographer = Choreographer.getInstance()
    private val lock = Any()

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!isRunning) return

            val cap = maxFps
            if (cap != null && !shouldRenderAt(frameTimeNanos, lastRenderNanos, cap)) {
                choreographer.postFrameCallbackDelayed(this, remainingDelayMillis(frameTimeNanos, lastRenderNanos, cap))
                return
            }
            lastRenderNanos = frameTimeNanos

            val shouldRender = isPlaying || !renderer.isIdle
            if (shouldRender) {
                requestRender()
                choreographer.postFrameCallback(this)
            } else {
                // Bars have settled while paused: stop the loop and cut the
                // audio analysis feed so CPU/GPU drop to ~0%.
                synchronized(lock) {
                    isRunning = false
                }
                processor?.enabled?.set(false)
            }
        }
    }

    /**
     * Starts the frame scheduling loop.
     */
    fun start() {
        processor?.enabled?.set(true)
        synchronized(lock) {
            if (!isRunning) {
                isRunning = true
                lastRenderNanos = 0L
                choreographer.postFrameCallback(frameCallback)
            }
        }
    }

    /**
     * Pauses frame scheduling and disables the audio processor.
     */
    fun stop() {
        processor?.enabled?.set(false)
        synchronized(lock) {
            if (isRunning) {
                isRunning = false
                choreographer.removeFrameCallback(frameCallback)
            }
        }
    }

    companion object {
        /**
         * Decides whether a frame at [nowNanos] may render given the last rendered
         * frame at [lastNanos] and the [maxFps] cap. The first frame after (re-)start
         * (`lastNanos == 0`) always renders.
         */
        fun shouldRenderAt(nowNanos: Long, lastNanos: Long, maxFps: Int): Boolean {
            if (maxFps <= 0) return true
            if (lastNanos == 0L) return true
            return nowNanos - lastNanos >= minIntervalNanos(maxFps)
        }

        /**
         * Delay in milliseconds until the next frame is due under the [maxFps] cap.
         */
        fun remainingDelayMillis(nowNanos: Long, lastNanos: Long, maxFps: Int): Long {
            if (maxFps <= 0) return 0L
            val dueIn = minIntervalNanos(maxFps) - (nowNanos - lastNanos)
            return (dueIn / 1_000_000L).coerceAtLeast(0L)
        }

        private fun minIntervalNanos(maxFps: Int): Long = (NANOS_PER_SECOND / maxFps).toLong()
    }
}
