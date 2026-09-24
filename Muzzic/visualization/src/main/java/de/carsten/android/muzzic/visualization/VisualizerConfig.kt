package de.carsten.android.muzzic.visualization

import kotlin.math.min

/**
 * Configuration parameters for the LED bar spectrum visualizer.
 *
 * @property bandCount Number of vertical spectrum bars (e.g., 32).
 * @property columnCount Number of visual columns in mirrored BARS mode (defaults to 2 * bandCount).
 * @property shimmerStrength Shimmer modulation on lit mirrored bars (0 disables the effect).
 * @property tipGlowStrength White highlight mix on the tip segment of mirrored bars (0 disables the effect).
 * @property segmentCount Number of LED segments per bar (e.g., 20).
 * @property ledHalfSize Half size of an LED segment in normalized cell proportions (e.g. 0.40, 0.34).
 * @property cornerRadius Relative corner radius for rounded LEDs (e.g. 0.25).
 * @property visualLatencyMs Latency compensation offset in milliseconds (default: 150 ms).
 * @property maxFps Optional FPS limit for rendering (null for native display refresh rate).
 * @property analysis Settings for the audio FFT and spectrum band analysis.
 * @property smoother Settings for bar attack, decay, and peak-hold dynamics.
 */
data class VisualizerConfig(
    val bandCount: Int = DEFAULT_SPECTRUM_BANDS,
    val columnCount: Int = DEFAULT_MIRRORED_COLUMNS,
    val shimmerStrength: Float = DEFAULT_SHIMMER_STRENGTH,
    val tipGlowStrength: Float = DEFAULT_TIP_GLOW_STRENGTH,
    val segmentCount: Int = DEFAULT_LED_SEGMENT_COUNT,
    val ledHalfSize: Pair<Float, Float> = Pair(0.40f, 0.34f),
    val cornerRadius: Float = 0.25f,
    val visualLatencyMs: Long = 150L,
    val maxFps: Int? = null,
    val analysis: AnalysisConfig = AnalysisConfig(),
    val smoother: SmootherConfig = SmootherConfig(),
)

/**
 * Analysis configuration for FFT processing and spectrum band normalization.
 *
 * @property fftSize Size of the FFT buffer (must be a power of two, default: 2048).
 * @property hopSize Hop size in samples between consecutive FFT analyses (default: 512).
 * @property fMinHz Minimum analysis frequency in Hz (default: 40 Hz).
 * @property fMaxHz Maximum analysis frequency in Hz (default: min(16000 Hz, 0.45 * sampleRate)).
 * @property floorDb Lower boundary of the dB display scale (default: -54 dB).
 * @property topDb Upper boundary of the dB display scale (default: -6 dB).
 * @property tiltDbPerOctave Spectral tilt boost per octave to compensate for pink noise roll-off (default: +3.0 dB).
 * @property agTargetTopDb Target top dB level for auto-gain adjustment (default: -6 dB).
 * @property agMaxGainDb Maximum allowable boost for auto-gain (default: 18 dB).
 * @property agReleaseDbPerSec Decay rate of the auto-gain reference level in dB/sec (default: 2.0 dB/s).
 */
data class AnalysisConfig(
    val fftSize: Int = 2048,
    val hopSize: Int = 512,
    val fMinHz: Float = 40f,
    val fMaxHz: Float = 16000f,
    val floorDb: Float = -54f,
    val topDb: Float = -6f,
    val tiltDbPerOctave: Float = 3.0f,
    val agTargetTopDb: Float = -6f,
    val agMaxGainDb: Float = 18f,
    val agReleaseDbPerSec: Float = 2.0f,
) {
    /**
     * Resolves the effective maximum analysis frequency based on the active sample rate.
     */
    fun effectiveFMax(sampleRate: Int): Float = min(fMaxHz, NYQUIST_SAFETY_MARGIN * sampleRate)
}

/**
 * Configuration for bar movement smoothing and peak-hold animation.
 *
 * @property fallPerSec Linear decay rate of spectrum bars per second (default: 1.6).
 * @property holdSec Hold duration for peak markers in seconds before dropping (default: 0.35 s).
 * @property peakFallPerSec Decay rate of peak markers per second after hold expires (default: 0.5).
 */
data class SmootherConfig(val fallPerSec: Float = 1.6f, val holdSec: Float = 0.35f, val peakFallPerSec: Float = 0.5f)
