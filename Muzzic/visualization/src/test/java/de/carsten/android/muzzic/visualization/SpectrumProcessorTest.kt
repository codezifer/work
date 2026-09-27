package de.carsten.android.muzzic.visualization

import de.carsten.android.muzzic.visualization.audio.BandMapper
import de.carsten.android.muzzic.visualization.audio.SpectrumProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.Test

private const val TEST_SAMPLE_RATE = 44100
private const val TEST_BAND_COUNT = 32

private fun BandMapper.bandContaining(freqHz: Float): Int {
    var best = 0
    var bestDist = Float.MAX_VALUE
    for (b in 0 until bandCount) {
        val dist = kotlin.math.abs(fCenter[b] - freqHz)
        if (dist < bestDist) {
            bestDist = dist
            best = b
        }
    }
    return best
}

private fun sine16BitStereo(frames: Int, freqHz: Float, amplitude: Float): ByteBuffer {
    val buffer = ByteBuffer.allocate(frames * 2 * 2).order(ByteOrder.LITTLE_ENDIAN)
    val shorts = buffer.asShortBuffer()
    for (i in 0 until frames) {
        val s = (amplitude * sin(2.0 * PI * freqHz * i / TEST_SAMPLE_RATE)).toFloat()
        val v = (s * 32767f).toInt().toShort()
        shorts.put(v)
        shorts.put(v)
    }
    buffer.position(0)
    return buffer
}

private fun sineFloatMono(frames: Int, freqHz: Float, amplitude: Float): ByteBuffer {
    val buffer = ByteBuffer.allocate(frames * 4).order(ByteOrder.LITTLE_ENDIAN)
    val floats = buffer.asFloatBuffer()
    for (i in 0 until frames) {
        floats.put((amplitude * sin(2.0 * PI * freqHz * i / TEST_SAMPLE_RATE)).toFloat())
    }
    buffer.position(0)
    return buffer
}

class SpectrumProcessorTest {

    @Test
    fun `16-bit stereo sine produces peak in expected band`() {
        val processor = SpectrumProcessor(bandCount = TEST_BAND_COUNT)
        processor.configure(TEST_SAMPLE_RATE, 2)

        processor.processAudio(sine16BitStereo(16384, 1000f, 0.5f), isFloat = false)

        val out = FloatArray(TEST_BAND_COUNT)
        val count = processor.bus.readAtOrBefore(Long.MAX_VALUE, out)
        assertThat(count).isEqualTo(TEST_BAND_COUNT)

        var argMax = 0
        for (b in 1 until TEST_BAND_COUNT) {
            if (out[b] > out[argMax]) argMax = b
        }
        assertThat(out[argMax]).`as`("Peak band value should be strong, was ${out[argMax]}").isGreaterThan(0.7f)

        val mapper = BandMapper(TEST_BAND_COUNT, TEST_SAMPLE_RATE, 2048, 40f, 16000f)
        val expected = mapper.bandContaining(1000f)
        assertThat(kotlin.math.abs(argMax - expected))
            .`as`("Peak band $argMax (center ${mapper.fCenter[argMax]}) should be near 1 kHz band $expected")
            .isLessThanOrEqualTo(1)
    }

    @Test
    fun `float mono sine produces peak in expected band`() {
        val processor = SpectrumProcessor(bandCount = TEST_BAND_COUNT)
        processor.configure(TEST_SAMPLE_RATE, 1)

        processor.processAudio(sineFloatMono(16384, 1000f, 0.5f), isFloat = true)

        val out = FloatArray(TEST_BAND_COUNT)
        val count = processor.bus.readAtOrBefore(Long.MAX_VALUE, out)
        assertThat(count).isEqualTo(TEST_BAND_COUNT)

        var argMax = 0
        for (b in 1 until TEST_BAND_COUNT) {
            if (out[b] > out[argMax]) argMax = b
        }
        assertThat(out[argMax]).`as`("Peak band value should be strong, was ${out[argMax]}").isGreaterThan(0.7f)
    }

    @Test
    fun `flush writes a zero frame and clears ghost peaks`() {
        val processor = SpectrumProcessor(bandCount = TEST_BAND_COUNT)
        processor.configure(TEST_SAMPLE_RATE, 2)
        processor.processAudio(sine16BitStereo(8192, 1000f, 0.5f), isFloat = false)

        processor.flush()

        val out = FloatArray(TEST_BAND_COUNT) { -1f }
        val count = processor.bus.readAtOrBefore(Long.MAX_VALUE, out)
        assertThat(count).isEqualTo(TEST_BAND_COUNT)
        for (b in 0 until TEST_BAND_COUNT) {
            assertThat(out[b]).`as`("Band $b should be 0 after flush").isCloseTo(0f, within(1e-6f))
        }
    }

    @Test
    fun `disabled processor writes nothing`() {
        val processor = SpectrumProcessor(bandCount = TEST_BAND_COUNT)
        processor.configure(TEST_SAMPLE_RATE, 2)
        processor.enabled.set(false)

        processor.processAudio(sine16BitStereo(8192, 1000f, 0.5f), isFloat = false)

        assertThat(processor.bus.newestTimestampNanos()).isEqualTo(0L)
    }
}
