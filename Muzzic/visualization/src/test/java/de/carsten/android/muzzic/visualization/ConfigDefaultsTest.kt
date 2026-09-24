package de.carsten.android.muzzic.visualization

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Guards the single source of truth for shared defaults: [VisualizerConfig]
 * must stay in sync with the named constants used across the module.
 */
class ConfigDefaultsTest {

    @Test
    fun `visualizer config defaults match named constants`() {
        val config = VisualizerConfig()
        assertEquals(DEFAULT_SPECTRUM_BANDS, config.bandCount)
        assertEquals(DEFAULT_MIRRORED_COLUMNS, config.columnCount)
        assertEquals(DEFAULT_SHIMMER_STRENGTH, config.shimmerStrength, 0f)
        assertEquals(DEFAULT_TIP_GLOW_STRENGTH, config.tipGlowStrength, 0f)
        assertEquals(DEFAULT_LED_SEGMENT_COUNT, config.segmentCount)
    }

    @Test
    fun `mirrored columns cover every band exactly twice`() {
        assertEquals(DEFAULT_SPECTRUM_BANDS * 2, DEFAULT_MIRRORED_COLUMNS)
    }
}
