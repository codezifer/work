package de.carsten.android.muzzic.visualization.component

import androidx.compose.ui.graphics.Color
import de.carsten.android.muzzic.visualization.BARS_SEGMENT_COUNT
import de.carsten.android.muzzic.visualization.DEFAULT_GLOW_STRENGTH
import de.carsten.android.muzzic.visualization.DEFAULT_SHIMMER_STRENGTH
import de.carsten.android.muzzic.visualization.DEFAULT_TIP_GLOW_STRENGTH
import de.carsten.android.muzzic.visualization.render.RenderStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VisualizerFactoryTest {

    @Test
    fun `bars resolves mirrored style with shimmer and glow`() {
        val definition = VisualizerFactory.resolve(
            VisualizerEngine.BARS,
            VisualizerParams.BarsParams(shimmerEnabled = true, tipGlowEnabled = true),
            Color.Red,
        ) as VisualizerDefinition.Spectrum

        assertEquals(RenderStyle.MIRRORED_BARS, definition.style)
        assertEquals(BARS_SEGMENT_COUNT, definition.config.segmentCount)
        assertEquals(DEFAULT_SHIMMER_STRENGTH, definition.config.shimmerStrength, 0f)
        assertEquals(DEFAULT_TIP_GLOW_STRENGTH, definition.config.tipGlowStrength, 0f)
        assertEquals(Color.Red, definition.theme.colLow)
    }

    @Test
    fun `bars disables effects on demand`() {
        val definition = VisualizerFactory.resolve(
            VisualizerEngine.BARS,
            VisualizerParams.BarsParams(shimmerEnabled = false, tipGlowEnabled = false),
            Color.Red,
        ) as VisualizerDefinition.Spectrum

        assertEquals(0f, definition.config.shimmerStrength, 0f)
        assertEquals(0f, definition.config.tipGlowStrength, 0f)
    }

    @Test
    fun `led spectrum resolves led style with album-art derived theme`() {
        val base = Color.Red
        val definition = VisualizerFactory.resolve(
            VisualizerEngine.LED_SPECTRUM,
            VisualizerParams.LedSpectrumParams,
            base,
        ) as VisualizerDefinition.Spectrum

        assertEquals(RenderStyle.LED, definition.style)
        assertEquals(Color.Transparent, definition.theme.background)
        assertEquals(base, definition.theme.colLow)
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

        assertEquals(DEFAULT_GLOW_STRENGTH, bars.config.glowStrength, 0f)
        assertEquals(DEFAULT_GLOW_STRENGTH, led.config.glowStrength, 0f)
    }

    @Test
    fun `projectm carries preset name`() {
        val definition = VisualizerFactory.resolve(
            VisualizerEngine.PROJECT_M,
            VisualizerParams.ProjectMParams("preset.milk"),
            Color.Red,
        ) as VisualizerDefinition.ProjectM

        assertEquals("preset.milk", definition.presetName)
    }

    @Test
    fun `projectm without preset resolves null preset`() {
        val definition = VisualizerFactory.resolve(
            VisualizerEngine.PROJECT_M,
            VisualizerParams.ProjectMParams(null),
            Color.Red,
        ) as VisualizerDefinition.ProjectM

        assertNull(definition.presetName)
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
