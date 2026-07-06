package de.carsten.android.muzzic.service.visualizer

import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A sink for audio data that performs FFT for visualization.
 */
@UnstableApi
class VisualizerSink : TeeAudioProcessor.AudioBufferSink {

    private val _amplitudes = MutableStateFlow<List<Float>>(emptyList())
    val amplitudes: StateFlow<List<Float>> = _amplitudes.asStateFlow()

    private val numBars = 32 // Increased from 16 to 32 for more detail
    private var previousBars = FloatArray(numBars) { 0f }
    private var sampleRate = 44100
    private var channelCount = 2

    override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
        this.sampleRate = sampleRateHz
        this.channelCount = channelCount
    }

    override fun handleBuffer(buffer: ByteBuffer) {
        val remaining = buffer.remaining()
        if (remaining == 0) return

        // Duplicate buffer to avoid modifying position of the original if needed
        // but handleBuffer is called by TeeAudioProcessor which expects us to read it.
        // Actually, TeeAudioProcessor just passes the buffer. We should probably not modify its position
        // if we want to be safe, but usually it's fine as we are the "tee".

        val localBuffer = buffer.duplicate().order(ByteOrder.nativeOrder())
        val shortBuffer = localBuffer.asShortBuffer()
        val availableSamples = shortBuffer.remaining()

        val windowSize = 1024 // Increased from 512 for better frequency resolution
        if (availableSamples < windowSize * channelCount) return

        val fftData = FloatArray(windowSize * 2)
        for (i in 0 until windowSize) {
            val monoSample = if (channelCount >= 2) {
                (shortBuffer.get().toFloat() + shortBuffer.get().toFloat()) / 2f
            } else {
                shortBuffer.get().toFloat()
            }
            // Apply Hanning window to reduce leakage
            val windowMultiplier = 0.5f * (1f - cos(2.0 * Math.PI * i / (windowSize - 1))).toFloat()
            fftData[i * 2] = monoSample * windowMultiplier
            fftData[i * 2 + 1] = 0f
        }

        performFft(fftData)

        // Magnitudes calculation, skipping the DC component (index 0) to avoid constant offset issues
        val magnitudes = FloatArray(windowSize / 2 - 1)
        for (i in 1 until windowSize / 2) {
            val re = fftData[i * 2]
            val im = fftData[i * 2 + 1]
            magnitudes[i - 1] = sqrt(re * re + im * im)
        }

        val bars = FloatArray(numBars)
        val binSize = magnitudes.size / numBars
        for (i in 0 until numBars) {
            var sum = 0f
            for (j in 0 until binSize) {
                sum += magnitudes[i * binSize + j]
            }
            bars[i] = sum / binSize
        }

        // Improved normalization: Use a fixed high-end threshold to prevent jumping on noise
        val noiseFloor = 100f
        val peakReference = 10000f

        val smoothedBars = bars.mapIndexed { index, amplitude ->
            val normalized = if (amplitude < noiseFloor) {
                0f
            } else {
                (amplitude / peakReference).coerceIn(0f, 1f)
            }

            // Temporal damping (smoothing)
            // Asymmetric damping: fast up, slower down for "snappier" but smooth feel
            val target = normalized
            val current = previousBars[index]
            val dampingFactor = if (target > current) 0.4f else 0.15f

            val smoothed = (current * (1f - dampingFactor)) + (target * dampingFactor)
            previousBars[index] = smoothed
            smoothed
        }

        _amplitudes.value = smoothedBars
    }

    private fun performFft(data: FloatArray) {
        val n = data.size / 2
        var j = 0
        for (i in 0 until n) {
            if (i < j) {
                val tempRe = data[i * 2]
                val tempIm = data[i * 2 + 1]
                data[i * 2] = data[j * 2]
                data[i * 2 + 1] = data[j * 2 + 1]
                data[j * 2] = tempRe
                data[j * 2 + 1] = tempIm
            }
            var m = n shr 1
            while (m >= 1 && j >= m) {
                j -= m
                m = m shr 1
            }
            j += m
        }

        var length = 2
        while (length <= n) {
            val angle = -2.0 * Math.PI / length
            val wLenRe = cos(angle).toFloat()
            val wLenIm = sin(angle).toFloat()
            var i = 0
            while (i < n) {
                var wRe = 1f
                var wIm = 0f
                for (k in 0 until length / 2) {
                    val uRe = data[(i + k) * 2]
                    val uIm = data[(i + k) * 2 + 1]
                    val vRe = data[(i + k + length / 2) * 2] * wRe - data[(i + k + length / 2) * 2 + 1] * wIm
                    val vIm = data[(i + k + length / 2) * 2] * wIm + data[(i + k + length / 2) * 2 + 1] * wRe
                    data[(i + k) * 2] = uRe + vRe
                    data[(i + k) * 2 + 1] = uIm + vIm
                    data[(i + k + length / 2) * 2] = uRe - vRe
                    data[(i + k + length / 2) * 2 + 1] = uIm - vIm
                    val nextWRe = wRe * wLenRe - wIm * wLenIm
                    wIm = wRe * wLenIm + wIm * wLenRe
                    wRe = nextWRe
                }
                i += length
            }
            length *= 2
        }
    }
}
