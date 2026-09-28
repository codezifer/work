package de.carsten.android.muzzic.visualization.audio

import de.carsten.android.muzzic.visualization.AnalysisConfig
import de.carsten.android.muzzic.visualization.DEFAULT_SAMPLE_RATE_HZ
import de.carsten.android.muzzic.visualization.DEFAULT_SPECTRUM_BANDS
import de.carsten.android.muzzic.visualization.MAX_ANALYSIS_DT_SEC
import de.carsten.android.muzzic.visualization.MIN_FRAME_DT_SEC
import de.carsten.android.muzzic.visualization.NANOS_PER_SECOND
import de.carsten.android.muzzic.visualization.bus.SpectrumBus
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * An allocation-free audio analyzer that processes PCM audio buffers from Media3, performs FFT,
 * band-mapping, and auto-gain, and writes normalized band outputs to [SpectrumBus].
 *
 * The FFT size is fixed at construction (must be a power of two); [configure] only
 * changes sample rate and channel count.
 */
class SpectrumProcessor(val bandCount: Int = DEFAULT_SPECTRUM_BANDS, val config: AnalysisConfig = AnalysisConfig(), val bus: SpectrumBus = SpectrumBus(maxBands = bandCount)) {
    val enabled = AtomicBoolean(true)

    private var sampleRate: Int = DEFAULT_SAMPLE_RATE_HZ
    private var channelCount: Int = 2

    // Bit mask for power-of-two ring indexing (faster than modulo per sample).
    private val ringMask = config.fftSize - 1

    private var pcmRingBuffer = FloatArray(config.fftSize)
    private var pcmWritePos = 0
    private var samplesSinceHop = 0

    private val hannWindow = FloatArray(config.fftSize)
    private var windowSum = 1f

    private val fft = Fft(config.fftSize)
    private val fftReal = FloatArray(config.fftSize)
    private val fftImag = FloatArray(config.fftSize)
    private val fftAmp = FloatArray(config.fftSize / 2 + 1)

    private var bandMapper = BandMapper(bandCount, sampleRate, config.fftSize, config.fMinHz, config.effectiveFMax(sampleRate))
    private var autoGain = AutoGain(bandCount, config, bandMapper.fCenter)

    private val bandAmpBuffer = FloatArray(bandCount)
    private val normalizedBuffer = FloatArray(bandCount)
    private val zeroBuffer = FloatArray(bandCount)

    private var lastAnalysisNanos: Long = System.nanoTime()

    init {
        require(config.fftSize > 0 && (config.fftSize and (config.fftSize - 1)) == 0) {
            "FFT size must be a positive power of 2, but was ${config.fftSize}"
        }
        recomputeWindow()
    }

    /**
     * Reconfigures audio parameters (e.g. sample rate or channel count).
     */
    fun configure(sampleRateHz: Int, channels: Int) {
        if (sampleRateHz <= 0 || channels <= 0) return
        if (sampleRate == sampleRateHz && channelCount == channels) return

        sampleRate = sampleRateHz
        channelCount = channels

        bandMapper = BandMapper(bandCount, sampleRate, config.fftSize, config.fMinHz, config.effectiveFMax(sampleRate))
        autoGain = AutoGain(bandCount, config, bandMapper.fCenter)
        resetBuffers()
    }

    /**
     * Resets analysis ring buffers and writes a zero frame to the bus (e.g. on seek or flush).
     */
    fun flush() {
        resetBuffers()
        autoGain.reset()
        bus.write(System.nanoTime(), zeroBuffer, bandCount)
    }

    /**
     * Processes an incoming PCM ByteBuffer (16-bit PCM or Float PCM).
     *
     * Operates view-only on [buffer]: neither its position nor its byte order is
     * modified. A buffer already in [order] is used directly without duplicating.
     *
     * @param buffer Direct or indirect ByteBuffer containing PCM audio.
     * @param isFloat Whether the PCM encoding is 32-bit Float (`true`) or 16-bit Int (`false`).
     * @param order Byte order of 16-bit samples (float PCM is always little-endian).
     */
    fun processAudio(buffer: ByteBuffer, isFloat: Boolean = false, order: ByteOrder = ByteOrder.LITTLE_ENDIAN) {
        if (!enabled.get()) return

        // Duplicate only to change byte order; views below never touch the caller's buffer.
        val ordered = if (buffer.order() == order) buffer else buffer.duplicate().order(order)
        val remainingBytes = ordered.remaining()
        if (remainingBytes <= 0) return

        val bytesPerSample = if (isFloat) 4 else 2
        val totalSamples = remainingBytes / bytesPerSample
        val frames = totalSamples / channelCount
        if (frames <= 0) return

        // Bounds are guaranteed by the frames cap, so no per-sample remaining checks.
        if (isFloat) {
            val floatBuf = ordered.asFloatBuffer()
            for (f in 0 until frames) {
                var sum = 0f
                for (c in 0 until channelCount) {
                    sum += floatBuf.get()
                }
                pushMonoSample(sum / channelCount)
            }
        } else {
            val shortBuf = ordered.asShortBuffer()
            for (f in 0 until frames) {
                var sum = 0f
                for (c in 0 until channelCount) {
                    sum += shortBuf.get().toFloat() / 32768f
                }
                pushMonoSample(sum / channelCount)
            }
        }
    }

    private fun pushMonoSample(sample: Float) {
        pcmRingBuffer[pcmWritePos] = sample
        pcmWritePos = (pcmWritePos + 1) and ringMask
        samplesSinceHop++

        if (samplesSinceHop >= config.hopSize) {
            samplesSinceHop = 0
            analyze()
        }
    }

    private fun analyze() {
        val now = System.nanoTime()
        val dt = ((now - lastAnalysisNanos) / NANOS_PER_SECOND).toFloat().coerceIn(MIN_FRAME_DT_SEC, MAX_ANALYSIS_DT_SEC)
        lastAnalysisNanos = now

        // 1. Copy last fftSize samples in order (two segments, no per-sample modulo)
        // and apply Hann window. pcmWritePos points at the oldest sample.
        val startPos = pcmWritePos
        val firstLen = config.fftSize - startPos
        System.arraycopy(pcmRingBuffer, startPos, fftReal, 0, firstLen)
        System.arraycopy(pcmRingBuffer, 0, fftReal, firstLen, startPos)
        for (i in 0 until config.fftSize) {
            fftReal[i] = fftReal[i] * hannWindow[i]
            fftImag[i] = 0f
        }

        // 2. Perform in-place FFT
        fft.transform(fftReal, fftImag)

        // 3. Magnitudes and Amplitude Normalization for bins 0..N/2
        val maxBin = config.fftSize / 2
        fftAmp[0] = sqrt(fftReal[0] * fftReal[0] + fftImag[0] * fftImag[0]) / windowSum
        for (k in 1 until maxBin) {
            val re = fftReal[k]
            val im = fftImag[k]
            val mag = sqrt(re * re + im * im)
            fftAmp[k] = (2f * mag) / windowSum
        }
        fftAmp[maxBin] = sqrt(fftReal[maxBin] * fftReal[maxBin] + fftImag[maxBin] * fftImag[maxBin]) / windowSum

        // 4. Band-Mapping
        bandMapper.map(fftAmp, bandAmpBuffer)

        // 5. Auto-Gain & Tilt
        autoGain.process(bandAmpBuffer, dt, normalizedBuffer)

        // 6. Push to SpectrumBus
        bus.write(now, normalizedBuffer, bandCount)
    }

    private fun recomputeWindow() {
        var sum = 0f
        val n = config.fftSize
        for (i in 0 until n) {
            val w = 0.5f - 0.5f * cos(2.0 * PI * i / (n - 1)).toFloat()
            hannWindow[i] = w
            sum += w
        }
        windowSum = if (sum > 0f) sum else 1f
    }

    private fun resetBuffers() {
        pcmRingBuffer.fill(0f)
        pcmWritePos = 0
        samplesSinceHop = 0
        lastAnalysisNanos = System.nanoTime()
    }
}
