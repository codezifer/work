package de.carsten.android.muzzic.visualization.component

import androidx.compose.ui.graphics.Color
import de.carsten.android.muzzic.visualization.BARS_SEGMENT_COUNT
import de.carsten.android.muzzic.visualization.DEFAULT_GLOW_STRENGTH
import de.carsten.android.muzzic.visualization.DEFAULT_SHIMMER_STRENGTH
import de.carsten.android.muzzic.visualization.DEFAULT_TIP_GLOW_STRENGTH
import de.carsten.android.muzzic.visualization.render.RenderStyle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class VisualizerFactoryTest {

    @Test
    fun `bars resolves mirrored style with shimmer and glow`() {
        val definition = VisualizerFactory.resolve(
            VisualizerEngine.BARS,
            VisualizerParams.BarsParams(shimmerEnabled = true, tipGlowEnabled = true),
            Color.Red,
        ) as VisualizerDefinition.Spectrum

        assertThat(definition.style).isEqualTo(RenderStyle.MIRRORED_BARS)
        assertThat(definition.config.segmentCount).isEqualTo(BARS_SEGMENT_COUNT)
        assertThat(definition.config.shimmerStrength).isEqualTo(DEFAULT_SHIMMER_STRENGTH)
        assertThat(definition.config.tipGlowStrength).isEqualTo(DEFAULT_TIP_GLOW_STRENGTH)
        assertThat(definition.theme.colLow).isEqualTo(Color.Red)
    }

    @Test
    fun `bars disables effects on demand`() {
        val definition = VisualizerFactory.resolve(
            VisualizerEngine.BARS,
            VisualizerParams.BarsParams(shimmerEnabled = false, tipGlowEnabled = false),
            Color.Red,
        ) as VisualizerDefinition.Spectrum

        assertThat(definition.config.shimmerStrength).isEqualTo(0f)
        assertThat(definition.config.tipGlowStrength).isEqualTo(0f)
    }

    @Test
    fun `led spectrum resolves led style with album-art derived theme`() {
        val base = Color.Red
        val definition = VisualizerFactory.resolve(
            VisualizerEngine.LED_SPECTRUM,
            VisualizerParams.LedSpectrumParams,
            base,
        ) as VisualizerDefinition.Spectrum

        assertThat(definition.style).isEqualTo(RenderStyle.LED)
        assertThat(definition.theme.background).isEqualTo(Color.Transparent)
        assertThat(definition.theme.colLow).isEqualTo(base)
    }

    @Test
    fun `spectrum definitions carry the default glow strength`() {
        val bars = VisualizerFactory.resolve(
            VisualizerEngine.BARS,
            VisualizerParams.BarsParams(shimmerEnabled = true, tipGlowEnabled = true),
            Color.Red,
        ) as VisualizerDefinition.Spectrum
        val led = VisualizerFactory.resolve(
            VisualizerEngine.LED_SPECTRUM,
            VisualizerParams.LedSpectrumParams,
            Color.Red,
        ) as VisualizerDefinition.Spectrum

        assertThat(bars.config.glowStrength).isEqualTo(DEFAULT_GLOW_STRENGTH)
        assertThat(led.config.glowStrength).isEqualTo(DEFAULT_GLOW_STRENGTH)
    }

    @Test
    fun `projectm carries preset name`() {
        val definition = VisualizerFactory.resolve(
            VisualizerEngine.PROJECT_M,
            VisualizerParams.ProjectMParams("preset.milk"),
            Color.Red,
        ) as VisualizerDefinition.ProjectM

        assertThat(definition.presetName).isEqualTo("preset.milk")
    }

    @Test
    fun `projectm without preset resolves null preset`() {
        val definition = VisualizerFactory.resolve(
            VisualizerEngine.PROJECT_M,
            VisualizerParams.ProjectMParams(null),
            Color.Red,
        ) as VisualizerDefinition.ProjectM

        assertThat(definition.presetName).isNull()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `mismatched params are rejected`() {
        VisualizerFactory.resolve(
            VisualizerEngine.BARS,
            VisualizerParams.ProjectMParams("preset.milk"),
            Color.Red,
        )
    }
}
