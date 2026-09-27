package de.carsten.android.muzzic.visualization

import de.carsten.android.muzzic.visualization.render.mirroredBandIndex
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class MirroredBarsTest {

    @Test
    fun `mirror mapping reproduces legacy bar ordering for 64 columns`() {
        // Legacy SegmentedBarsVisualizer with 64 bars:
        // left half shows bands 31..0 (bass in the center), right half 0..31.
        for (column in 0 until 64) {
            val expected = if (column < 32) 31 - column else column - 32
            assertThat(mirroredBandIndex(column, columnCount = 64, bandCount = 32))
                .`as`("Column $column should map to band $expected")
                .isEqualTo(expected)
        }
    }

    @Test
    fun `mirror mapping is symmetric around the center`() {
        for (column in 0 until 32) {
            assertThat(mirroredBandIndex(column, 64, 32))
                .`as`("Columns $column and ${63 - column} should mirror")
                .isEqualTo(mirroredBandIndex(63 - column, 64, 32))
        }
    }

    @Test
    fun `mirror mapping clamps to available bands`() {
        for (column in 0 until 64) {
            val band = mirroredBandIndex(column, columnCount = 64, bandCount = 16)
            assertThat(band).`as`("Band $band out of range for column $column").isBetween(0, 15)
        }
        // Bass columns still resolve to the lowest bands.
        assertThat(mirroredBandIndex(31, 64, 16)).isEqualTo(0)
        assertThat(mirroredBandIndex(32, 64, 16)).isEqualTo(0)
    }

    @Test
    fun `mirror mapping works for small layouts`() {
        // 8 columns over 4 bands: 3,2,1,0 | 0,1,2,3.
        val expected = listOf(3, 2, 1, 0, 0, 1, 2, 3)
        for (column in 0 until 8) {
            assertThat(mirroredBandIndex(column, 8, 4)).isEqualTo(expected[column])
        }
    }
}
