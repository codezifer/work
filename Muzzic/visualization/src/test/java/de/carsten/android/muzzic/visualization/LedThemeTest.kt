package de.carsten.android.muzzic.visualization

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LedThemeTest {

    @Test
    fun `led theme keeps base color as low zone with transparent background`() {
        val base = Color.Red
        val theme = VisualizerTheme.ledThemeFrom(base)

        assertEquals(base, theme.colLow)
        assertEquals(Color.Transparent, theme.background)
    }

    @Test
    fun `green base reproduces the classic green-yellow-red ladder`() {
        val theme = VisualizerTheme.ledThemeFrom(Color(0f, 1f, 0f))

        val midHsl = VisualizerTheme.rgbToHsl(theme.colMid.red, theme.colMid.green, theme.colMid.blue)
        val highHsl = VisualizerTheme.rgbToHsl(theme.colHigh.red, theme.colHigh.green, theme.colHigh.blue)

        assertEquals(60f, midHsl[0], 1f)
        assertEquals(0f, highHsl[0], 1f)
        assertTrue(midHsl[2] > 0.5f)
        assertTrue(highHsl[2] > midHsl[2])
    }

    @Test
    fun `achromatic base falls back to a lightness ladder`() {
        val gray = Color(0.4f, 0.4f, 0.4f)
        val theme = VisualizerTheme.ledThemeFrom(gray)

        assertEquals(gray, theme.colLow)
        assertNotEquals(gray, theme.colMid)
        assertTrue(theme.colMid.red > gray.red)
        assertTrue(theme.colHigh.red > theme.colMid.red)
    }

    @Test
    fun `led theme rotates mid and high zones and preserves alpha`() {
        val base = Color.Red.copy(alpha = 0.4f)
        val theme = VisualizerTheme.ledThemeFrom(base)

        assertNotEquals("Mid zone should differ from base by hue shift", base, theme.colMid)
        assertNotEquals("High zone should differ from base by hue shift", base, theme.colHigh)
        assertNotEquals("Mid and high zones should differ from each other", theme.colMid, theme.colHigh)
        assertEquals(base.alpha, theme.colMid.alpha, 1e-6f)
        assertEquals(base.alpha, theme.colHigh.alpha, 1e-6f)
    }
}
