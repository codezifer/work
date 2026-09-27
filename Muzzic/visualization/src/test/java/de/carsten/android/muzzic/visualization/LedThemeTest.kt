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

class LedThemeTest {

    @Test
    fun `led theme keeps base color as low zone with transparent background`() {
        val base = Color.Red
        val theme = VisualizerTheme.ledThemeFrom(base)

        assertThat(theme.colLow).isEqualTo(base)
        assertThat(theme.background).isEqualTo(Color.Transparent)
    }

    @Test
    fun `green base reproduces the classic green-yellow-red ladder`() {
        val theme = VisualizerTheme.ledThemeFrom(Color(0f, 1f, 0f))

        val midHsl = VisualizerTheme.rgbToHsl(theme.colMid.red, theme.colMid.green, theme.colMid.blue)
        val highHsl = VisualizerTheme.rgbToHsl(theme.colHigh.red, theme.colHigh.green, theme.colHigh.blue)

        assertThat(midHsl[0]).isCloseTo(60f, within(1f))
        assertThat(highHsl[0]).isCloseTo(0f, within(1f))
        assertThat(midHsl[2]).isGreaterThan(0.5f)
        assertThat(highHsl[2]).isGreaterThan(midHsl[2])
    }

    @Test
    fun `gray album-art accent becomes vivid instead of gray`() {
        val gray = Color(0.4f, 0.4f, 0.4f)
        val theme = VisualizerTheme.ledThemeFrom(gray)
        val lowHsl = VisualizerTheme.rgbToHsl(theme.colLow.red, theme.colLow.green, theme.colLow.blue)

        assertThat(lowHsl[1]).isCloseTo(0.5f, within(COLOR_EPS))
        assertThat(theme.colLow).isNotEqualTo(gray)
        assertThat(theme.colLow).isNotEqualTo(theme.colMid)
    }

    @Test
    fun `near-white album-art accent stays visibly colored`() {
        val pale = Color(0.95f, 0.93f, 0.9f)
        val theme = VisualizerTheme.ledThemeFrom(pale)
        val lowHsl = VisualizerTheme.rgbToHsl(theme.colLow.red, theme.colLow.green, theme.colLow.blue)

        assertThat(lowHsl[1]).isCloseTo(0.5f, within(COLOR_EPS))
        assertThat(lowHsl[2]).isCloseTo(0.65f, within(COLOR_EPS))
    }

    @Test
    fun `led theme rotates mid and high zones and preserves alpha`() {
        val base = Color.Red.copy(alpha = 0.4f)
        val theme = VisualizerTheme.ledThemeFrom(base)

        assertThat(theme.colMid).`as`("Mid zone should differ from base by hue shift").isNotEqualTo(base)
        assertThat(theme.colHigh).`as`("High zone should differ from base by hue shift").isNotEqualTo(base)
        assertThat(theme.colMid).`as`("Mid and high zones should differ from each other").isNotEqualTo(theme.colHigh)
        assertThat(theme.colMid.alpha).isCloseTo(base.alpha, within(1e-6f))
        assertThat(theme.colHigh.alpha).isCloseTo(base.alpha, within(1e-6f))
    }
}
