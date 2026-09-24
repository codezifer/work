package de.carsten.android.muzzic.visualization.audio

import de.carsten.android.muzzic.visualization.AMPLITUDE_TO_DB_FACTOR
import de.carsten.android.muzzic.visualization.AnalysisConfig
import de.carsten.android.muzzic.visualization.DIGITAL_SILENCE_THRESHOLD_DB
import de.carsten.android.muzzic.visualization.INITIAL_FRAME_MAX_DB
import de.carsten.android.muzzic.visualization.MIN_LINEAR_AMPLITUDE
import de.carsten.android.muzzic.visualization.TILT_REFERENCE_HZ
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.max

/**
 * Handles dB scaling, +3 dB/octave spectral tilt compensation, and adaptive auto-gain control.
 *
 * Ensures tracks with varying volume levels display vibrant visual feedback without amplifying
 * silence or noise floors during pauses.
 */
class AutoGain(val bandCount: Int, val config: AnalysisConfig, fCenterHz: FloatArray) {
    private val tiltDb: FloatArray = FloatArray(bandCount)
    private var trackDb: Float = config.agTargetTopDb

    init {
        for (b in 0 until bandCount) {
            // Guard against log(0) for the DC bin; 1 Hz is far below any band center.
            val fCenter = max(1f, fCenterHz[b])
            tiltDb[b] = config.tiltDbPerOctave * log2(fCenter / TILT_REFERENCE_HZ)
        }
    }

    /**
     * Resets internal auto-gain tracking state smoothly (e.g. on track flush or seek).
     */
    fun reset() {
        trackDb = config.agTargetTopDb
    }

    /**
     * Normalizes linear band amplitudes into 0.0..1.0 values with tilt and auto-gain.
     *
     * @param bandAmp Linear band amplitudes input.
     * @param dt Elapsed time in seconds since the last analysis frame.
     * @param outNormalized Target array for normalized 0.0..1.0 output values.
     */
    fun process(bandAmp: FloatArray, dt: Float, outNormalized: FloatArray) {
        var frameMaxDb = INITIAL_FRAME_MAX_DB

        // 1. Convert to dB and apply spectral tilt
        for (b in 0 until bandCount) {
            val amp = max(bandAmp[b], MIN_LINEAR_AMPLITUDE)
            val dbVal = (AMPLITUDE_TO_DB_FACTOR * log10(amp)) + tiltDb[b]
            outNormalized[b] = dbVal
            if (dbVal > frameMaxDb) {
                frameMaxDb = dbVal
            }
        }

        // 2. Track peak dB level and adjust auto-gain
        val gainDb: Float
        if (frameMaxDb < DIGITAL_SILENCE_THRESHOLD_DB) {
            // Digital silence: disable gain boost and reset trackDb
            trackDb = config.agTargetTopDb
            gainDb = 0f
        } else {
            val decayedTrackDb = trackDb - config.agReleaseDbPerSec * dt
            trackDb = max(decayedTrackDb, frameMaxDb)
            val desiredGain = config.agTargetTopDb - trackDb
            gainDb = desiredGain.coerceIn(0f, config.agMaxGainDb)
        }

        // 3. Normalize into [0.0, 1.0] range
        val range = config.topDb - config.floorDb
        for (b in 0 until bandCount) {
            val totalDb = outNormalized[b] + gainDb
            val norm = (totalDb - config.floorDb) / range
            outNormalized[b] = norm.coerceIn(0f, 1f)
        }
    }
}
