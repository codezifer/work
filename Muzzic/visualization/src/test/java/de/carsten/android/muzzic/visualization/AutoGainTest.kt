package de.carsten.android.muzzic.visualization

import de.carsten.android.muzzic.visualization.audio.AutoGain
import de.carsten.android.muzzic.visualization.audio.BandMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.Test

private fun autoGainSetup(bandCount: Int = 32, sampleRate: Int = 44100, fftSize: Int = 2048): Pair<AnalysisConfig, FloatArray> {
    val config = AnalysisConfig()
    val mapper = BandMapper(bandCount, sampleRate, fftSize, config.fMinHz, config.effectiveFMax(sampleRate))
    return config to mapper.fCenter
}

class AutoGainTest {

    @Test
    fun `digital silence produces all-zero output`() {
        val (config, fCenter) = autoGainSetup()
        val autoGain = AutoGain(32, config, fCenter)
        val bandAmp = FloatArray(32)
        val out = FloatArray(32) { -1f }

        autoGain.process(bandAmp, 0.0116f, out)

        for (b in 0 until 32) {
            assertThat(out[b]).`as`("Band $b should be 0 in silence").isCloseTo(0f, within(1e-6f))
        }
    }

    @Test
    fun `loud sine near full scale applies no gain and peaks at one`() {
        val (config, fCenter) = autoGainSetup()
        val autoGain = AutoGain(32, config, fCenter)
        // Band 17 sits near 1 kHz where the +3 dB/octave tilt is ~0 dB.
        val bandAmp = FloatArray(32)
        bandAmp[17] = 0.7f // ~-3 dBFS
        val out = FloatArray(32)

        autoGain.process(bandAmp, 0.0116f, out)

        assertThat(out[17]).isCloseTo(1f, within(1e-4f))
        for (b in 0 until 32) {
            assertThat(out[b]).`as`("Band $b must stay in 0..1").isBetween(0f, 1f)
        }
    }

    @Test
    fun `quiet sine is boosted over time but gain never exceeds max`() {
        val (config, fCenter) = autoGainSetup()
        val autoGain = AutoGain(32, config, fCenter)
        val bandAmp = FloatArray(32)
        bandAmp[17] = 0.01f // -40 dBFS
        val out = FloatArray(32)
        val dt = 512f / 44100f // one hop at 44.1 kHz

        // Plain normalization without gain would give (-40 + 54) / 48 = ~0.29.
        // Let the tracker converge: 3000 hops ~ 35 s of audio, release is 2 dB/s.
        repeat(3000) {
            autoGain.process(bandAmp, dt, out)
        }

        // Fully converged gain would be min(18, -6 + 40) = 18 dB,
        // giving (-40 + 18 + 54) / 48 = ~0.667.
        assertThat(out[17]).`as`("Quiet sine should be boosted above plain 0.29, was ${out[17]}").isGreaterThan(0.35f)
        assertThat(out[17]).`as`("Gain must be capped: output ${out[17]} exceeds 18 dB boost").isLessThan(0.75f)
        for (b in 0 until 32) {
            assertThat(out[b]).`as`("Band $b must stay in 0..1").isBetween(0f, 1f)
        }
    }

    @Test
    fun `reset restores initial tracking state`() {
        val (config, fCenter) = autoGainSetup()
        val autoGain = AutoGain(32, config, fCenter)
        val loud = FloatArray(32)
        loud[17] = 0.7f
        val out = FloatArray(32)

        autoGain.process(loud, 0.0116f, out)
        autoGain.reset()

        val silence = FloatArray(32)
        autoGain.process(silence, 0.0116f, out)
        for (b in 0 until 32) {
            assertThat(out[b]).`as`("Band $b should be 0 after reset + silence").isCloseTo(0f, within(1e-6f))
        }
    }
}
