package de.carsten.android.muzzic.visualization

/**
 * Preview Dark Mode Constant
 */
const val PREVIEW_DARK_MODE = 0x20

/**
 * Hue shift in degrees applied to derive the mirrored-BARS edge color.
 */
const val VISUALIZER_HUE_COLOR_DEGREE = 40

// ---------------------------------------------------------------------------
// Spectrum geometry limits
// ---------------------------------------------------------------------------

/** Minimum supported spectrum band count (debug source and bus sizing). */
const val MIN_SPECTRUM_BANDS = 8

/** Default spectrum band count for analysis, smoothing, and LED towers. */
const val DEFAULT_SPECTRUM_BANDS = 32

/** Maximum spectrum band count; sizes all preallocated band buffers. */
const val MAX_SPECTRUM_BANDS = 64

/** Visual columns in mirrored-BARS mode (bass mirrored around the center). */
const val DEFAULT_MIRRORED_COLUMNS = 64

/** Total LED rows in mirrored-BARS mode, split into top/bottom halves. */
const val BARS_SEGMENT_COUNT = 16

/** Total LED rows in LED-tower mode. */
const val DEFAULT_LED_SEGMENT_COUNT = 20

/** Default shimmer modulation on lit mirrored bars. */
const val DEFAULT_SHIMMER_STRENGTH = 0.08f

/** Default white highlight mix on mirrored bar tip segments. */
const val DEFAULT_TIP_GLOW_STRENGTH = 0.35f

/** Default outer halo strength around lit segments (0 disables the effect). */
const val DEFAULT_GLOW_STRENGTH = 0.8f

/** Hue rotation in degrees for the LED mid zone relative to the base color. */
const val LED_MID_HUE_SHIFT_DEG = -60

/** Hue rotation in degrees for the LED high zone relative to the base color. */
const val LED_HIGH_HUE_SHIFT_DEG = -120

/** Lightness boost for the LED mid zone relative to the base color. */
const val LED_MID_LIGHTNESS_BOOST = 0.05f

/** Lightness boost for the LED high zone relative to the base color. */
const val LED_HIGH_LIGHTNESS_BOOST = 0.1f

/** Preallocated spectrum frame slots; 64 frames cover ~740 ms at ~86 analyses/s. */
const val SPECTRUM_BUS_CAPACITY_FRAMES = 64

// ---------------------------------------------------------------------------
// Time conversions and frame-time sanity bounds
// ---------------------------------------------------------------------------

/** Nanoseconds per millisecond, for latency compensation math. */
const val NANOS_PER_MILLISECOND = 1_000_000L

/** Nanoseconds per second (Double, for timestamp delta division). */
const val NANOS_PER_SECOND = 1_000_000_000.0

/** Lower dt clamp: ignore sub-millisecond deltas in smoothing/integration. */
const val MIN_FRAME_DT_SEC = 0.001f

/** Upper dt clamp for render-side smoothing (GL and Canvas fallback). */
const val MAX_RENDER_DT_SEC = 0.05f

/** Upper dt clamp for audio analysis steps (allows slower analysis hops). */
const val MAX_ANALYSIS_DT_SEC = 0.1f

/**
 * Grace period beyond the latency offset after which the newest bus frame
 * counts as stale (pause, buffering, track end) and bars decay to zero.
 */
const val STALE_FRAME_GRACE_NANOS = 120_000_000L

/** Bars and peaks below this value count as settled for render-loop auto-stop. */
const val RENDERER_IDLE_VALUE_THRESHOLD = 0.01f

/** Peak markers at or below this value stay hidden. */
const val PEAK_VISIBILITY_THRESHOLD = 0.02f

// ---------------------------------------------------------------------------
// Audio analysis reference values
// ---------------------------------------------------------------------------

/** Fallback sample rate before the first audio format is configured. */
const val DEFAULT_SAMPLE_RATE_HZ = 44100

/** Peak magnitude of 16-bit PCM; normalizes samples to the −1..1 range. */
const val PCM_16BIT_PEAK_AMPLITUDE = 32768f

/** Reference frequency for the spectral tilt curve (+3 dB/octave above 1 kHz). */
const val TILT_REFERENCE_HZ = 1000f

/** Floor for linear amplitudes before dB conversion; avoids log(0). */
const val MIN_LINEAR_AMPLITUDE = 1e-6f

/** Initial frame-maximum dB, lower than any real signal. */
const val INITIAL_FRAME_MAX_DB = -160f

/** Below this frame level the input counts as digital silence (no auto-gain). */
const val DIGITAL_SILENCE_THRESHOLD_DB = -80f

/** Amplitude-to-dB factor in `db = 20 * log10(amp)`. */
const val AMPLITUDE_TO_DB_FACTOR = 20f

/**
 * Safety margin below Nyquist for the top analysis frequency
 * (`fMax = min(configured, margin * sampleRate)`).
 */
const val NYQUIST_SAFETY_MARGIN = 0.45f

// ---------------------------------------------------------------------------
// Theme zone defaults
// ---------------------------------------------------------------------------

/** Fractional bar height where the mid (yellow) zone starts. */
const val DEFAULT_ZONE_MID_START = 0.60f

/** Fractional bar height where the high (red) zone starts. */
const val DEFAULT_ZONE_HIGH_START = 0.85f

/** Full hue wheel span in degrees, for wrapping hue rotations. */
const val HUE_WHEEL_DEG = 360f

/** Maximum lightness value in HSL conversions. */
const val MAX_LIGHTNESS = 1f

/** Saturation below which a color counts as achromatic (grays). */
const val ACHROMATIC_SATURATION_THRESHOLD = 0.05f

// ---------------------------------------------------------------------------
// Platform and debug values
// ---------------------------------------------------------------------------

/** `reqGlEsVersion` value for OpenGL ES 3.0 capable devices. */
const val GL_ES_3_0_VERSION_CODE = 0x00030000

/** Synthetic debug frame interval (~86 Hz, matching the analysis hop rate). */
const val SYNTH_FRAME_INTERVAL_MICROS = 11600L

/** Age of the pre-filled preview bus frame, past the latency window. */
const val PREVIEW_SYNTH_FRAME_AGE_NANOS = 200_000_000L
