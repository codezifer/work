package de.carsten.android.muzzic.visualization

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * Guards the single source of truth for shared defaults: [VisualizerConfig]
 * must stay in sync with the named constants used across the module.
 */
class ConfigDefaultsTest {

    @Test
    fun `visualizer config defaults match named constants`() {
        val config = VisualizerConfig()
        assertThat(config.bandCount).isEqualTo(DEFAULT_SPECTRUM_BANDS)
        assertThat(config.columnCount).isEqualTo(DEFAULT_MIRRORED_COLUMNS)
        assertThat(config.shimmerStrength).isEqualTo(DEFAULT_SHIMMER_STRENGTH)
        assertThat(config.tipGlowStrength).isEqualTo(DEFAULT_TIP_GLOW_STRENGTH)
        assertThat(config.glowStrength).isEqualTo(DEFAULT_GLOW_STRENGTH)
        assertThat(config.hotCoreStrength).isEqualTo(DEFAULT_HOT_CORE_STRENGTH)
        assertThat(config.specularStrength).isEqualTo(DEFAULT_SPECULAR_STRENGTH)
        assertThat(config.bleedStrength).isEqualTo(DEFAULT_BLEED_STRENGTH)
        assertThat(config.glowGradeStrength).isEqualTo(DEFAULT_GLOW_GRADE_STRENGTH)
        assertThat(config.fadeStrength).isEqualTo(DEFAULT_FADE_STRENGTH)
        assertThat(config.trailStrength).isEqualTo(DEFAULT_TRAIL_STRENGTH)
        assertThat(config.segmentCount).isEqualTo(DEFAULT_LED_SEGMENT_COUNT)
    }

    @Test
    fun `mirrored columns cover every band exactly twice`() {
        assertThat(DEFAULT_SPECTRUM_BANDS * 2).isEqualTo(DEFAULT_MIRRORED_COLUMNS)
    }
}
