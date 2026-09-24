package de.carsten.android.muzzic.visualization.audio

import de.carsten.android.muzzic.visualization.NYQUIST_SAFETY_MARGIN
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Maps linear FFT bin amplitudes to logarithmic frequency bands with energy averaging and
 * linear interpolation for narrow low-frequency (bass) bands.
 *
 * Pre-calculates bin boundary tables and interpolation weights during configuration to guarantee
 * zero object allocation in execution hot paths.
 */
class BandMapper(val bandCount: Int, sampleRate: Int, fftSize: Int, fMinHz: Float = 40f, fMaxHz: Float = 16000f) {
    val fCenter: FloatArray = FloatArray(bandCount)
    private val loBins: IntArray = IntArray(bandCount)
    private val hiBins: IntArray = IntArray(bandCount)
    private val interpBin0: IntArray = IntArray(bandCount)
    private val interpBin1: IntArray = IntArray(bandCount)
    private val interpWeight1: FloatArray = FloatArray(bandCount)

    init {
        val df = sampleRate.toFloat() / fftSize.toFloat()
        // Stay clear of the Nyquist frequency where anti-alias filters roll off.
        val effectiveFMax = min(fMaxHz, NYQUIST_SAFETY_MARGIN * sampleRate)
        val clampedFMin = max(1f, fMinHz)

        val edge = FloatArray(bandCount + 1)
        val ratio = effectiveFMax / clampedFMin

        for (i in 0..bandCount) {
            edge[i] = clampedFMin * ratio.pow(i.toFloat() / bandCount.toFloat())
        }

        val maxBin = fftSize / 2

        for (b in 0 until bandCount) {
            val f0 = edge[b]
            val f1 = edge[b + 1]
            fCenter[b] = sqrt(f0 * f1)

            val lo = ceil(f0 / df).toInt().coerceIn(0, maxBin)
            val hi = floor(f1 / df).toInt().coerceIn(0, maxBin)

            if (hi >= lo) {
                loBins[b] = lo
                hiBins[b] = hi
                interpBin0[b] = -1
                interpBin1[b] = -1
                interpWeight1[b] = 0f
            } else {
                // Narrow band: linear interpolation at fractional bin position
                val fracBin = fCenter[b] / df
                val b0 = floor(fracBin).toInt().coerceIn(0, maxBin)
                val b1 = (b0 + 1).coerceIn(0, maxBin)
                val w1 = fracBin - b0

                loBins[b] = -1
                hiBins[b] = -1
                interpBin0[b] = b0
                interpBin1[b] = b1
                interpWeight1[b] = w1
            }
        }
    }

    /**
     * Maps the FFT bin amplitudes in [fftAmp] to logarithmic spectrum band amplitudes into [outBandAmp].
     *
     * @param fftAmp Normalized bin amplitudes for bins 0..N/2.
     * @param outBandAmp Target array for mapped band amplitudes (length must equal [bandCount]).
     */
    fun map(fftAmp: FloatArray, outBandAmp: FloatArray) {
        for (b in 0 until bandCount) {
            val lo = loBins[b]
            if (lo >= 0) {
                val hi = hiBins[b]
                var sumSq = 0f
                val count = hi - lo + 1
                for (k in lo..hi) {
                    val a = fftAmp[k]
                    sumSq += a * a
                }
                outBandAmp[b] = sqrt(sumSq / count)
            } else {
                val b0 = interpBin0[b]
                val b1 = interpBin1[b]
                val w1 = interpWeight1[b]
                val a0 = fftAmp[b0]
                val a1 = fftAmp[b1]
                outBandAmp[b] = a0 + w1 * (a1 - a0)
            }
        }
    }
}
