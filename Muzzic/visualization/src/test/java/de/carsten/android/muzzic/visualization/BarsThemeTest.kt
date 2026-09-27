package de.carsten.android.muzzic.visualization

import androidx.compose.ui.graphics.Color
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.Test

/**
 * Tolerance for HSL values round-tripped through Compose [Color], which
 * quantizes sRGB channels to 8 bit (one step is 1/255).
 */
private const val COLOR_EPS = 0.01f

class BarsThemeTest {

    @Test
    fun `bars theme keeps base color and transparent background`() {
        val base = Color.Red
        val theme = VisualizerTheme.barsThemeFrom(base)

        assertThat(theme.colLow).isEqualTo(base)
        assertThat(theme.background).isEqualTo(Color.Transparent)
        assertThat(theme.offIntensity).isCloseTo(0f, within(1e-6f))
    }

    @Test
    fun `bars theme deepens washed-out album-art accents`() {
        val pale = Color(0.95f, 0.93f, 0.9f)
        val theme = VisualizerTheme.barsThemeFrom(pale)
        val hsl = VisualizerTheme.rgbToHsl(theme.colLow.red, theme.colLow.green, theme.colLow.blue)

        assertThat(hsl[1]).isCloseTo(0.5f, within(COLOR_EPS))
        assertThat(hsl[2]).isCloseTo(0.65f, within(COLOR_EPS))
    }

    @Test
    fun `vivid colors pass through unchanged`() {
        val vivid = Color.Red
        val theme = VisualizerTheme.barsThemeFrom(vivid)

        assertThat(theme.colLow).isEqualTo(vivid)
    }

    @Test
    fun `bars theme shifts target hue and preserves alpha`() {
        val base = Color.Red.copy(alpha = 0.4f)
        val theme = VisualizerTheme.barsThemeFrom(base)

        assertThat(theme.colMid).`as`("Target should differ from base by hue shift").isNotEqualTo(base)
        assertThat(theme.colMid.alpha).isCloseTo(base.alpha, within(1e-6f))
        assertThat(theme.colHigh.alpha).isCloseTo(base.alpha, within(1e-6f))
    }

    @Test
    fun `rgb-hsl round trip preserves primary colors`() {
        val primaries = listOf(
            Triple(1f, 0f, 0f) to 0f,
            Triple(0f, 1f, 0f) to 120f,
            Triple(0f, 0f, 1f) to 240f,
        )
        for ((rgb, expectedHue) in primaries) {
            val hsl = VisualizerTheme.rgbToHsl(rgb.first, rgb.second, rgb.third)
            assertThat(hsl[0]).isCloseTo(expectedHue, within(1e-3f))
            val back = VisualizerTheme.hslToColor(hsl[0], hsl[1], hsl[2])
            assertThat(back.red).isCloseTo(rgb.first, within(1e-4f))
            assertThat(back.green).isCloseTo(rgb.second, within(1e-4f))
            assertThat(back.blue).isCloseTo(rgb.third, within(1e-4f))
        }
    }

    @Test
    fun `hue shift moves red towards orange`() {
        val hsl = VisualizerTheme.rgbToHsl(1f, 0f, 0f)
        val shifted = VisualizerTheme.hslToColor(hsl[0] + 40f, hsl[1], hsl[2])
        assertThat(shifted.red).isCloseTo(1f, within(1e-4f))
        assertThat(shifted.green).`as`("Shifted red should gain green, was ${shifted.green}").isGreaterThan(0.5f)
        assertThat(shifted.blue).isCloseTo(0f, within(1e-4f))
    }
}
