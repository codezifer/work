package de.carsten.android.muzzic.visualization

import de.carsten.android.muzzic.visualization.audio.Fft
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FftTest {

    @Test
    fun `FFT produces correct peak bin for pure sine wave`() {
        val fftSize = 64
        val fft = Fft(fftSize)

        val targetBin = 8
        val real = FloatArray(fftSize)
        val imag = FloatArray(fftSize)

        for (i in 0 until fftSize) {
            val angle = 2.0 * PI * targetBin * i / fftSize
            real[i] = sin(angle).toFloat()
        }

        fft.transform(real, imag)

        val mag = FloatArray(fftSize / 2)
        var maxBin = 0
        var maxMag = 0f

        for (k in 0 until fftSize / 2) {
            val m = sqrt(real[k] * real[k] + imag[k] * imag[k])
            mag[k] = m
            if (m > maxMag) {
                maxMag = m
                maxBin = k
            }
        }

        assertEquals(targetBin, maxBin)
        assertTrue(maxMag > 10f)
    }

    @Test
    fun `FFT handles zero array without NaN`() {
        val fftSize = 128
        val fft = Fft(fftSize)

        val real = FloatArray(fftSize)
        val imag = FloatArray(fftSize)

        fft.transform(real, imag)

        for (i in 0 until fftSize) {
            assertEquals(0f, real[i], 1e-6f)
            assertEquals(0f, imag[i], 1e-6f)
        }
    }
}
