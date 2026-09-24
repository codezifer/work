package de.carsten.android.muzzic.visualization.service

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import de.carsten.android.muzzic.visualization.PCM_16BIT_PEAK_AMPLITUDE
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
 */
@UnstableApi
class VisualizerSink : TeeAudioProcessor.AudioBufferSink {

    val spectrumProcessor = SpectrumProcessor()
    val spectrumBus: SpectrumBus get() = spectrumProcessor.bus

    private var currentEncoding: Int = C.ENCODING_PCM_16BIT

    private val projectMWindowSize = 1024
    private var channelCount = 2

    override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
        this.channelCount = channelCount
        this.currentEncoding = encoding
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
        if (encodingSupported) {
            spectrumProcessor.processAudio(buffer, isFloat = isFloat)
        }

        feedProjectM(buffer, isFloat)
    }

    /**
     * Forwards PCM samples to the native projectM instance when active.
     * Never throws: audio-thread failures must not break playback.
     */
    private fun feedProjectM(buffer: ByteBuffer, isFloat: Boolean) {
        if (!ProjectMNativeBridge.isActive) return
        try {
            val count = projectMWindowSize * channelCount
            val pcmFloats = FloatArray(count)
            if (isFloat) {
                val floatBuffer = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
                for (i in 0 until count) {
                    if (floatBuffer.hasRemaining()) {
                        pcmFloats[i] = floatBuffer.get()
                    }
                }
            } else {
                val shortBuffer = buffer.duplicate().order(ByteOrder.nativeOrder()).asShortBuffer()
                for (i in 0 until count) {
                    if (shortBuffer.hasRemaining()) {
                        pcmFloats[i] = shortBuffer.get().toFloat() / PCM_16BIT_PEAK_AMPLITUDE
                    }
                }
            }
            ProjectMNativeBridge.addPcm(pcmFloats, channelCount)
        } catch (_: Exception) {
            // Ignore: visualization must never break playback.
        }
    }
}
