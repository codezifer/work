import de.carsten.android.muzzic.visualization.audio.BandMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class BandMapperTest {

    @Test
    fun `BandMapper center frequencies are strictly monotonically increasing`() {
        val bandMapper = BandMapper(
            bandCount = 32,
            sampleRate = 44100,
            fftSize = 2048,
            fMinHz = 40f,
            fMaxHz = 16000f,
        )

        for (i in 0 until bandMapper.bandCount - 1) {
            assertThat(bandMapper.fCenter[i])
                .`as`("Center frequency at band $i (${bandMapper.fCenter[i]}) should be < next band (${bandMapper.fCenter[i + 1]})")
                .isLessThan(bandMapper.fCenter[i + 1])
        }
    }

    @Test
    fun `BandMapper maps single bin signal to corresponding band`() {
        val bandCount = 16
        val fftSize = 512
        val sampleRate = 44100
        val bandMapper = BandMapper(bandCount, sampleRate, fftSize)

        val fftAmp = FloatArray(fftSize / 2 + 1)
        val targetBin = 10
        fftAmp[targetBin] = 1.0f

        val outBandAmp = FloatArray(bandCount)
        bandMapper.map(fftAmp, outBandAmp)

        var activeBands = 0
        for (b in 0 until bandCount) {
            if (outBandAmp[b] > 0f) activeBands++
        }

        assertThat(activeBands).`as`("At least one band should receive amplitude").isGreaterThan(0)
    }

    @Test
    fun `BandMapper centers stay monotonic and in range for 48 and 96 kHz`() {
        for (sampleRate in listOf(48000, 96000)) {
            val bandMapper = BandMapper(
                bandCount = 32,
                sampleRate = sampleRate,
                fftSize = 2048,
                fMinHz = 40f,
                fMaxHz = 16000f,
            )
            for (i in 0 until bandMapper.bandCount - 1) {
                assertThat(bandMapper.fCenter[i])
                    .`as`("Center at $sampleRate Hz band $i should increase")
                    .isLessThan(bandMapper.fCenter[i + 1])
            }
            assertThat(bandMapper.fCenter[0]).isGreaterThan(40f)
            assertThat(bandMapper.fCenter[bandMapper.bandCount - 1]).isLessThan(16000f)
        }
    }

    @Test
    fun `BandMapper interpolates narrow bass bands instead of returning zero`() {
        // At 44.1 kHz / 2048 FFT the bin resolution (~21.5 Hz) is wider than
        // the lowest log bands, forcing the hi < lo interpolation path.
        val bandMapper = BandMapper(
            bandCount = 32,
            sampleRate = 44100,
            fftSize = 2048,
            fMinHz = 40f,
            fMaxHz = 16000f,
        )
        val fftAmp = FloatArray(2048 / 2 + 1)
        fftAmp[2] = 1.0f
        fftAmp[3] = 1.0f

        val outBandAmp = FloatArray(32)
        bandMapper.map(fftAmp, outBandAmp)

        assertThat(outBandAmp[1]).`as`("Narrow bass band 1 should interpolate, was ${outBandAmp[1]}").isGreaterThan(0f)
        for (b in 0 until 32) {
            assertThat(outBandAmp[b]).`as`("Band $b must not be NaN").isNotNaN()
        }
    }

    @Test
    fun `BandMapper puts 1 kHz tone into the band containing 1 kHz`() {
        for (sampleRate in listOf(44100, 48000, 96000)) {
            val bandCount = 32
            val fftSize = 2048
            val bandMapper = BandMapper(bandCount, sampleRate, fftSize, 40f, 16000f)
            val df = sampleRate.toFloat() / fftSize
            val bin = (1000f / df).toInt().coerceIn(0, fftSize / 2)

            val fftAmp = FloatArray(fftSize / 2 + 1)
            fftAmp[bin] = 1.0f
            if (bin > 0) fftAmp[bin - 1] = 0.5f
            if (bin < fftSize / 2) fftAmp[bin + 1] = 0.5f

            val out = FloatArray(bandCount)
            bandMapper.map(fftAmp, out)

            var argMax = 0
            for (b in 1 until bandCount) {
                if (out[b] > out[argMax]) argMax = b
            }
            var expected = 0
            var bestDist = Float.MAX_VALUE
            for (b in 0 until bandCount) {
                val dist = kotlin.math.abs(bandMapper.fCenter[b] - 1000f)
                if (dist < bestDist) {
                    bestDist = dist
                    expected = b
                }
            }
            assertThat(kotlin.math.abs(argMax - expected))
                .`as`("At $sampleRate Hz peak band $argMax should be near 1 kHz band $expected")
                .isLessThanOrEqualTo(1)
        }
    }
}
