package de.carsten.android.muzzic.visualization

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BarsThemeTest {

    @Test
    fun `bars theme keeps base color and transparent background`() {
        val base = Color.Red
        val theme = VisualizerTheme.barsThemeFrom(base)

        assertEquals(base, theme.colLow)
        assertEquals(Color.Transparent, theme.background)
        assertEquals(0f, theme.offIntensity, 0f)
    }

    @Test
    fun `bars theme shifts target hue and preserves alpha`() {
        val base = Color.Red.copy(alpha = 0.4f)
        val theme = VisualizerTheme.barsThemeFrom(base)

        assertNotEquals("Target should differ from base by hue shift", base, theme.colMid)
        assertEquals(base.alpha, theme.colMid.alpha, 1e-6f)
        assertEquals(base.alpha, theme.colHigh.alpha, 1e-6f)
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
            assertEquals(expectedHue, hsl[0], 1e-3f)
            val back = VisualizerTheme.hslToColor(hsl[0], hsl[1], hsl[2])
            assertEquals(rgb.first, back.red, 1e-4f)
            assertEquals(rgb.second, back.green, 1e-4f)
            assertEquals(rgb.third, back.blue, 1e-4f)
        }
    }

    @Test
    fun `hue shift moves red towards orange`() {
        val hsl = VisualizerTheme.rgbToHsl(1f, 0f, 0f)
        val shifted = VisualizerTheme.hslToColor(hsl[0] + 40f, hsl[1], hsl[2])
        assertEquals(1f, shifted.red, 1e-4f)
        assertTrue("Shifted red should gain green, was ${shifted.green}", shifted.green > 0.5f)
        assertEquals(0f, shifted.blue, 1e-4f)
    }
}
