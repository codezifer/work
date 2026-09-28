package de.carsten.android.muzzic.visualization.render

import de.carsten.android.muzzic.visualization.MAX_RENDER_DT_SEC
import de.carsten.android.muzzic.visualization.MAX_SPECTRUM_BANDS
import de.carsten.android.muzzic.visualization.MIN_FRAME_DT_SEC
import de.carsten.android.muzzic.visualization.SmootherConfig
import kotlin.math.max
import kotlin.math.min

/**
 * Handles time-compensated bar attack, linear decay, and peak-hold dynamics.
 *
 * Fully deterministic pure-Kotlin state holder, allowing execution and unit testing on JVM.
 *
 * @param maxBands Maximum number of spectrum bars supported.
 */
class LedBarSmoother(val maxBands: Int = MAX_SPECTRUM_BANDS) {
    val bands = FloatArray(maxBands)
    val peaks = FloatArray(maxBands)
    private val hold = FloatArray(maxBands)

    /**
     * Resets all bar, peak, and hold values to zero.
     */
    fun reset() {
        bands.fill(0f)
        peaks.fill(0f)
        hold.fill(0f)
    }

    /**
     * Updates bar and peak values based on elapsed time [dtSec] and target spectrum values.     *
     * @param target Array of target normalized band values (0.0..1.0).
     * @param bandCount Active number of bands in [target].
     * @param dtSec Elapsed time in seconds since last frame.
     * @param cfg Smoothing velocity and timing parameters.
     */
    fun update(target: FloatArray, bandCount: Int, dtSec: Float, cfg: SmootherConfig) {
        val dt = min(MAX_RENDER_DT_SEC, max(MIN_FRAME_DT_SEC, dtSec))
        val activeBands = min(maxBands, bandCount)

        for (b in 0 until activeBands) {
            val tgt = target[b].coerceIn(0f, 1f)
            var bar = bands[b]
            var peak = peaks[b]
            var h = hold[b]

            // 1. Attack / Decay for main bar
            if (tgt >= bar) {
                bar = tgt
            } else {
                bar = max(tgt, bar - cfg.fallPerSec * dt)
            }

            // 2. Peak-Hold logic
            if (bar >= peak) {
                peak = bar
                h = cfg.holdSec
            } else {
                if (h > 0f) {
                    h -= dt
                } else {
                    peak = max(bar, peak - cfg.peakFallPerSec * dt)
                }
            }

            bands[b] = bar.coerceIn(0f, 1f)
            peaks[b] = peak.coerceIn(0f, 1f)
            hold[b] = max(0f, h)
        }

        // Clear any remaining bands outside active count
        for (b in activeBands until maxBands) {
            bands[b] = 0f
            peaks[b] = 0f
            hold[b] = 0f
        }
    }

    /**
     * Returns the maximum over active bar and peak values.
     *
     * Used by render loops to detect the settled (idle) state.
     *
     * @param bandCount Active number of bands.
     */
    fun maxValue(bandCount: Int): Float {
        var maxVal = 0f
        val activeBands = min(maxBands, bandCount)
        for (b in 0 until activeBands) {
            if (bands[b] > maxVal) maxVal = bands[b]
            if (peaks[b] > maxVal) maxVal = peaks[b]
        }
        return maxVal
    }
}
