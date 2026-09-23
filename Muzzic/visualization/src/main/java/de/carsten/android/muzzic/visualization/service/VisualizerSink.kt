package de.carsten.android.muzzic.visualization.service

import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import de.carsten.android.muzzic.visualization.projectm.ProjectMNativeBridge
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

    private val numBars = 64
    private val windowSize = 1024
    private var previousBars = FloatArray(numBars) { 0f }
    private var fftData = FloatArray(windowSize * 2)
    private var magnitudes = FloatArray(windowSize / 2 - 1)
    private var bars = FloatArray(numBars)
    private var sampleRate = 44100
    private var channelCount = 2
    private var runningPeak = 0.5f

    override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
        this.sampleRate = sampleRateHz
        this.channelCount = channelCount
    }

    override fun handleBuffer(buffer: ByteBuffer) {
        val remaining = buffer.remaining()
        if (remaining == 0) return

        val localBuffer = buffer.duplicate().order(ByteOrder.nativeOrder())
        val shortBuffer = localBuffer.asShortBuffer()
        val availableSamples = shortBuffer.remaining()

        if (availableSamples < windowSize * channelCount) return

        // Extract PCM float samples for projectM audio processing if native instance is active
        if (ProjectMNativeBridge.isActive) {
            val pcmFloats = FloatArray(windowSize * channelCount)
            val pcmReadBuffer = shortBuffer.duplicate()
            for (i in pcmFloats.indices) {
                if (pcmReadBuffer.hasRemaining()) {
                    pcmFloats[i] = pcmReadBuffer.get().toFloat() / 32768f
                }
            }
            ProjectMNativeBridge.addPcm(pcmFloats, channelCount)
        }

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

        // Magnitudes calculation
        for (i in 1 until windowSize / 2) {
            val re = fftData[i * 2]
            val im = fftData[i * 2 + 1]
            magnitudes[i - 1] = sqrt(re * re + im * im)
        }

        val binSize = magnitudes.size / numBars
        for (i in 0 until numBars) {
            var sum = 0f
            for (j in 0 until binSize) {
                sum += magnitudes[i * binSize + j]
            }
            bars[i] = (sum / binSize) / windowSize
        }

        val currentMax = bars.maxOrNull() ?: 0f
        runningPeak = if (currentMax > runningPeak) {
            currentMax
        } else {
            (runningPeak * 0.97f).coerceAtLeast(0.01f)
        }

        val smoothedBars = ArrayList<Float>(numBars)
        for (index in 0 until numBars) {
            val amplitude = bars[index]
            val normalized = (amplitude / (runningPeak * 0.4f)).coerceIn(0f, 1f)
            val current = previousBars[index]
            val dampingFactor = if (normalized > current) 0.7f else 0.3f
            val smoothed = (current * (1f - dampingFactor)) + (normalized * dampingFactor)
            previousBars[index] = smoothed
            smoothedBars.add(smoothed)
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
            while (m in 1..j) {
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
