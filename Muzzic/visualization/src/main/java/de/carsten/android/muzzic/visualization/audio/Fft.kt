package de.carsten.android.muzzic.visualization.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * An allocation-free, in-place Radix-2 Cooley-Tukey Fast Fourier Transform implementation.
 *
 * Pre-computes bit-reversal and twiddle factor tables during initialization to ensure zero GC allocation
 * in hot analysis loops.
 *
 * @param size The FFT buffer size N (must be a positive power of 2, e.g. 2048).
 */
class Fft(val size: Int) {

    init {
        require(size > 0 && (size and (size - 1)) == 0) {
            "FFT size must be a positive power of 2, but was $size"
        }
    }

    private val bitReverse = IntArray(size)
    private val twiddleCos = FloatArray(size / 2)
    private val twiddleSin = FloatArray(size / 2)

    init {
        // Bit-reversal table: mirror each index across all significant bits.
        // Int.SIZE_BITS (32) minus the bit-width of the FFT size gives the shift
        // that keeps only the significant bits of the reversed integer.
        var shift = 1
        var tempSize = size shr 1
        while (tempSize > 0) {
            shift++
            tempSize = tempSize shr 1
        }
        shift = Int.SIZE_BITS - shift + 1

        for (i in 0 until size) {
            bitReverse[i] = Integer.reverse(i) ushr shift
        }

        // Pre-compute twiddle factors W_N^k = exp(-2*pi*i*k / N)
        for (k in 0 until size / 2) {
            val angle = -2.0 * PI * k / size
            twiddleCos[k] = cos(angle).toFloat()
            twiddleSin[k] = sin(angle).toFloat()
        }
    }

    /**
     * Executes an in-place forward Radix-2 FFT on the provided [real] and [imag] arrays.
     *
     * @param real Array of real component inputs/outputs (length must equal [size]).
     * @param imag Array of imaginary component inputs/outputs (length must equal [size]).
     */
    fun transform(real: FloatArray, imag: FloatArray) {
        require(real.size >= size && imag.size >= size) {
            "Input arrays must be at least of length $size"
        }

        // Bit-reversal permutation
        for (i in 0 until size) {
            val j = bitReverse[i]
            if (i < j) {
                val tempR = real[i]
                real[i] = real[j]
                real[j] = tempR

                val tempI = imag[i]
                imag[i] = imag[j]
                imag[j] = tempI
            }
        }

        // Cooley-Tukey Radix-2 FFT computation
        var len = 2
        while (len <= size) {
            val halfLen = len shr 1
            val step = size / len

            var i = 0
            while (i < size) {
                var twiddleIdx = 0
                for (k in 0 until halfLen) {
                    val wRe = twiddleCos[twiddleIdx]
                    val wIm = twiddleSin[twiddleIdx]

                    val posEven = i + k
                    val posOdd = i + k + halfLen

                    val evenRe = real[posEven]
                    val evenIm = imag[posEven]

                    val oddRe = real[posOdd]
                    val oddIm = imag[posOdd]

                    val termRe = oddRe * wRe - oddIm * wIm
                    val termIm = oddRe * wIm + oddIm * wRe

                    real[posEven] = evenRe + termRe
                    imag[posEven] = evenIm + termIm

                    real[posOdd] = evenRe - termRe
                    imag[posOdd] = evenIm - termIm

                    twiddleIdx += step
                }
                i += len
            }
            len = len shl 1
        }
    }
}
