package de.carsten.android.muzzic.visualization.service

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import de.carsten.android.muzzic.visualization.PCM_16BIT_PEAK_AMPLITUDE
import de.carsten.android.muzzic.visualization.PROJECTM_ACTIVE_CHECK_INTERVAL_NANOS
import de.carsten.android.muzzic.visualization.PROJECTM_WINDOW_SIZE
import de.carsten.android.muzzic.visualization.audio.SpectrumProcessor
import de.carsten.android.muzzic.visualization.bus.SpectrumBus
import de.carsten.android.muzzic.visualization.projectm.ProjectMNativeBridge
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * A sink for audio data feeding the GLES spectrum analysis and projectM.
 *
 * All visualizers (mirrored BARS, LED spectrum) read normalized logarithmic bands
 * from [spectrumBus]; no legacy FFT path remains.
 *
 * Hot-path notes: [handleBuffer] runs on the ExoPlayer audio thread. The PCM
 * scratch array for projectM is reused across calls (reallocated only when the
 * channel count changes), the input buffer is duplicated once per call, and the
 * native `isActive` state is cached for [PROJECTM_ACTIVE_CHECK_INTERVAL_NANOS]
 * instead of crossing JNI per buffer.
 */
@UnstableApi
class VisualizerSink : TeeAudioProcessor.AudioBufferSink {

    val spectrumProcessor = SpectrumProcessor()
    val spectrumBus: SpectrumBus get() = spectrumProcessor.bus

    private var currentEncoding: Int = C.ENCODING_PCM_16BIT

    private var channelCount = 2

    /** Reused PCM scratch for the projectM feed; sized `PROJECTM_WINDOW_SIZE * channelCount`. */
    private var pcmScratch = FloatArray(PROJECTM_WINDOW_SIZE * channelCount)

    @Volatile private var cachedProjectMActive = false

    @Volatile private var lastActiveCheckNanos: Long = 0L

    override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
        this.channelCount = channelCount
        this.currentEncoding = encoding
        ensureScratch(channelCount)
        spectrumProcessor.configure(sampleRateHz, channelCount)
        spectrumProcessor.flush()
    }

    override fun handleBuffer(buffer: ByteBuffer) {
        if (buffer.remaining() == 0) return

        // Feed GLES spectrum analysis (16-bit and float only; other encodings
        // such as offloaded/tunneled passthrough leave the display empty).
        val isFloat = currentEncoding == C.ENCODING_PCM_FLOAT
        val encodingSupported =
            isFloat ||
                currentEncoding == C.ENCODING_PCM_16BIT ||
                currentEncoding == C.ENCODING_PCM_16BIT_BIG_ENDIAN
        // Single duplicate shared by both consumers; views derived from it never
        // touch the original buffer's position or byte order.
        val order = if (currentEncoding == C.ENCODING_PCM_16BIT_BIG_ENDIAN) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN
        val shared = buffer.duplicate().order(order)
        if (encodingSupported) {
            spectrumProcessor.processAudio(shared.duplicate(), isFloat = isFloat, order = order)
        }

        feedProjectM(shared.duplicate(), isFloat)
    }

    private fun ensureScratch(channels: Int) {
        val needed = PROJECTM_WINDOW_SIZE * channels.coerceAtLeast(1)
        if (pcmScratch.size != needed) {
            pcmScratch = FloatArray(needed)
        }
    }

    /**
     * Returns the cached native active flag, refreshing it over JNI at most every
     * [PROJECTM_ACTIVE_CHECK_INTERVAL_NANOS]. Never throws: audio-thread failures
     * must not break playback.
     */
    private fun isProjectMActiveCached(nowNanos: Long): Boolean {
        val lastCheck = lastActiveCheckNanos
        if (nowNanos - lastCheck >= PROJECTM_ACTIVE_CHECK_INTERVAL_NANOS) {
            val active = try {
                ProjectMNativeBridge.isActive
            } catch (_: Exception) {
                false
            }
            cachedProjectMActive = active
            lastActiveCheckNanos = nowNanos
        }
        return cachedProjectMActive
    }

    /**
     * Forwards PCM samples to the native projectM instance when active.
     * Never throws: audio-thread failures must not break playback.
     *
     * Fills the reused [pcmScratch] up to `PROJECTM_WINDOW_SIZE` samples per
     * channel and zero-fills the remainder so no stale data leaks into projectM.
     * Only 16-bit little-endian and 32-bit float PCM are forwarded; big-endian
     * input was already rejected for the spectrum path and stays silent here.
     */
    private fun feedProjectM(shared: ByteBuffer, isFloat: Boolean) {
        if (!isProjectMActiveCached(System.nanoTime())) return
        try {
            val channels = channelCount.coerceIn(1, 2)
            ensureScratch(channels)
            val count = PROJECTM_WINDOW_SIZE * channels
            val pcmFloats = pcmScratch
            if (isFloat) {
                val floatBuffer = shared.asFloatBuffer()
                for (i in 0 until count) {
                    pcmFloats[i] = if (floatBuffer.hasRemaining()) floatBuffer.get() else 0f
                }
            } else {
                if (currentEncoding == C.ENCODING_PCM_16BIT_BIG_ENDIAN) return
                val shortBuffer = shared.asShortBuffer()
                for (i in 0 until count) {
                    pcmFloats[i] = if (shortBuffer.hasRemaining()) {
                        shortBuffer.get().toFloat() / PCM_16BIT_PEAK_AMPLITUDE
                    } else {
                        0f
                    }
                }
            }
            ProjectMNativeBridge.addPcm(pcmFloats, channels)
        } catch (_: Exception) {
            // Ignore: visualization must never break playback.
        }
    }
}
