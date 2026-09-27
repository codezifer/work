package de.carsten.android.muzzic.visualization.component

import androidx.compose.ui.graphics.Color
import de.carsten.android.muzzic.visualization.VisualizerTheme

/**
 * Color source for the BARS and LED spectrum visualizers.
 *
 * Either follows the current album-art accent color dynamically or uses a fixed
 * hue spanning the color wheel. Fixed entries share full saturation and medium
 * lightness for a vivid LED look.
 *
 * @property hueDeg Fixed hue in degrees (0..360), or null for [ALBUM_ART].
 */
enum class VisualizerColorSource(val hueDeg: Float?) {
    /** Follows the current album-art accent color. */
    ALBUM_ART(null),

    /** Fixed red. */
    RED(0f),

    /** Fixed orange. */
    ORANGE(40f),

    /** Fixed yellow. */
    YELLOW(60f),

    /** Fixed 80s HiFi green. */
    GREEN_80S(120f),

    /** Fixed cyan. */
    CYAN(180f),

    /** Fixed ice blue. */
    ICE_BLUE(200f),

    /** Fixed blue. */
    BLUE(240f),

    /** Fixed magenta. */
    MAGENTA(300f),
    ;

    /**
     * Resolves the base color for themes derived from this source.
     *
     * @param albumArt Current album-art accent color, used for [ALBUM_ART].
     * @return album-art color for [ALBUM_ART], otherwise the fixed hue.
     */
    fun baseColor(albumArt: Color): Color {
        val hue = hueDeg ?: return albumArt
        return VisualizerTheme.hslToColor(hue, FULL_SATURATION, MEDIUM_LIGHTNESS)
    }

    private companion object {
        const val FULL_SATURATION = 1f
        const val MEDIUM_LIGHTNESS = 0.5f
    }
}
