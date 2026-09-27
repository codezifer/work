package de.carsten.android.muzzic.visualization.component

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class VisualizerColorSourceTest {

    @Test
    fun `album art passes the accent color through`() {
        val albumArt = Color.Red.copy(alpha = 0.4f)

        assertEquals(albumArt, VisualizerColorSource.ALBUM_ART.baseColor(albumArt))
    }

    @Test
    fun `fixed sources span the color wheel with full alpha`() {
        val albumArt = Color.Red.copy(alpha = 0.4f)
        val bases = VisualizerColorSource.entries
            .filter { it != VisualizerColorSource.ALBUM_ART }
            .map { it.baseColor(albumArt) }

        assertEquals(8, bases.size)
        assertEquals(8, bases.toSet().size)
        bases.forEach { assertEquals(1f, it.alpha, 1e-6f) }
    }

    @Test
    fun `green 80s is green dominant`() {
        val green = VisualizerColorSource.GREEN_80S.baseColor(Color.White)

        assertNotEquals(green.red, green.green)
        assert(green.green > green.red)
        assert(green.green > green.blue)
    }
}
