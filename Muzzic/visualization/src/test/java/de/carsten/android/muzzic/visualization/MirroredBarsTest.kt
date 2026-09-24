package de.carsten.android.muzzic.visualization

import de.carsten.android.muzzic.visualization.render.mirroredBandIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MirroredBarsTest {

    @Test
    fun `mirror mapping reproduces legacy bar ordering for 64 columns`() {
        // Legacy SegmentedBarsVisualizer with 64 bars:
        // left half shows bands 31..0 (bass in the center), right half 0..31.
        for (column in 0 until 64) {
            val expected = if (column < 32) 31 - column else column - 32
            assertEquals(
                "Column $column should map to band $expected",
                expected,
                mirroredBandIndex(column, columnCount = 64, bandCount = 32),
            )
        }
    }

    @Test
    fun `mirror mapping is symmetric around the center`() {
        for (column in 0 until 32) {
            assertEquals(
                "Columns $column and ${63 - column} should mirror",
                mirroredBandIndex(column, 64, 32),
                mirroredBandIndex(63 - column, 64, 32),
            )
        }
    }

    @Test
    fun `mirror mapping clamps to available bands`() {
        for (column in 0 until 64) {
            val band = mirroredBandIndex(column, columnCount = 64, bandCount = 16)
            assertTrue("Band $band out of range for column $column", band in 0..15)
        }
        // Bass columns still resolve to the lowest bands.
        assertEquals(0, mirroredBandIndex(31, 64, 16))
        assertEquals(0, mirroredBandIndex(32, 64, 16))
    }

    @Test
    fun `mirror mapping works for small layouts`() {
        // 8 columns over 4 bands: 3,2,1,0 | 0,1,2,3.
        val expected = listOf(3, 2, 1, 0, 0, 1, 2, 3)
        for (column in 0 until 8) {
            assertEquals(expected[column], mirroredBandIndex(column, 8, 4))
        }
    }
}
