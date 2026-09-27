package de.carsten.android.muzzic.visualization.component

import androidx.compose.ui.graphics.Color
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.Test

class VisualizerColorSourceTest {

    @Test
    fun `album art passes the accent color through`() {
        val albumArt = Color.Red.copy(alpha = 0.4f)

        assertThat(VisualizerColorSource.ALBUM_ART.baseColor(albumArt)).isEqualTo(albumArt)
    }

    @Test
    fun `fixed sources span the color wheel with full alpha`() {
        val albumArt = Color.Red.copy(alpha = 0.4f)
        val bases = VisualizerColorSource.entries
            .filter { it != VisualizerColorSource.ALBUM_ART }
            .map { it.baseColor(albumArt) }

        assertThat(bases).hasSize(8)
        assertThat(bases.toSet()).hasSize(8)
        bases.forEach { assertThat(it.alpha).isCloseTo(1f, within(1e-6f)) }
    }

    @Test
    fun `green 80s is green dominant`() {
        val green = VisualizerColorSource.GREEN_80S.baseColor(Color.White)

        assertThat(green.red).isNotEqualTo(green.green)
        assertThat(green.green).isGreaterThan(green.red)
        assertThat(green.green).isGreaterThan(green.blue)
    }
}
