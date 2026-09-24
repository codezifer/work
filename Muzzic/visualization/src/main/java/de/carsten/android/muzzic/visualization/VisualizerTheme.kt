package de.carsten.android.muzzic.visualization

import androidx.compose.ui.graphics.Color

/**
 * Defines visual styling and color zones for the LED bar spectrum analyzer.
 *
 * @property colLow Color for the low amplitude zone (e.g., green).
 * @property colMid Color for the medium amplitude zone (e.g., yellow/amber).
 * @property colHigh Color for the high amplitude zone (e.g., red/magenta).
 * @property zoneStart Fractional boundaries for zones: `x` is yellow start (0..1), `y` is red start (0..1).
 * @property background Background color of the visualizer canvas/surface.
 * @property offIntensity Brightness multiplier for inactive LED segments (e.g. 0.06).
 */
data class VisualizerTheme(
    val colLow: Color,
    val colMid: Color,
    val colHigh: Color,
    val zoneStart: Pair<Float, Float> = Pair(DEFAULT_ZONE_MID_START, DEFAULT_ZONE_HIGH_START),
    val background: Color = Color.Black,
    val offIntensity: Float = 0.06f,
) {
    companion object {
        /** Classic HiFi Green -> Yellow -> Red LED theme. */
        val ClassicGreen = VisualizerTheme(
            colLow = Color(0xFF00FF44),
            colMid = Color(0xFFFFDD00),
            colHigh = Color(0xFFFF2200),
        )

        /** Vintage Amber LED theme. */
        val Amber = VisualizerTheme(
            colLow = Color(0xFFFFB000),
            colMid = Color(0xFFFFCC00),
            colHigh = Color(0xFFFFAA00),
        )

        /** Modern Ice Blue -> Cyan -> White LED theme. */
        val IceBlue = VisualizerTheme(
            colLow = Color(0xFF00E5FF),
            colMid = Color(0xFF80D8FF),
            colHigh = Color(0xFFFFFFFF),
        )

        /**
         * Derives a mirrored-BARS theme from a Material accent color.
         *
         * The base color is used at the vertical center, fading towards a
         * hue-shifted target at the outer edges (same gradient as the legacy
         * Canvas bars). Background stays transparent so the host layout shows through.
         */
        fun barsThemeFrom(base: Color): VisualizerTheme {
            val hsl = rgbToHsl(base.red, base.green, base.blue)
            hsl[0] = (hsl[0] + VISUALIZER_HUE_COLOR_DEGREE.toFloat()) % 360f
            val target = hslToColor(hsl[0], hsl[1], hsl[2]).copy(alpha = base.alpha)
            return VisualizerTheme(
                colLow = base,
                colMid = target,
                colHigh = target,
                background = Color.Transparent,
                offIntensity = 0f,
            )
        }

        /**
         * Pure-Kotlin RGB to HSL conversion (h in 0..360, s/l in 0..1).
         *
         * Avoids `androidx.core.graphics.ColorUtils`, which calls into
         * `android.graphics.Color` and is therefore unusable in JVM unit tests.
         */
        internal fun rgbToHsl(r: Float, g: Float, b: Float): FloatArray {
            val max = maxOf(r, g, b)
            val min = minOf(r, g, b)
            val l = (max + min) / 2f
            if (max == min) return floatArrayOf(0f, 0f, l)
            val d = max - min
            val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
            var h = when (max) {
                r -> (g - b) / d + (if (g < b) 6f else 0f)
                g -> (b - r) / d + 2f
                else -> (r - g) / d + 4f
            }
            h *= 60f
            return floatArrayOf(h, s, l)
        }

        /**
         * Pure-Kotlin HSL to RGB conversion, counterpart to [rgbToHsl].
         */
        internal fun hslToColor(h: Float, s: Float, l: Float): Color {
            if (s == 0f) return Color(l, l, l)
            val hh = ((h % 360f) + 360f) % 360f / 360f
            val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
            val p = 2f * l - q
            return Color(hueToRgb(p, q, hh + 1f / 3f), hueToRgb(p, q, hh), hueToRgb(p, q, hh - 1f / 3f))
        }

        private fun hueToRgb(p: Float, q: Float, t: Float): Float {
            val tt = when {
                t < 0f -> t + 1f
                t > 1f -> t - 1f
                else -> t
            }
            return when {
                tt < 1f / 6f -> p + (q - p) * 6f * tt
                tt < 1f / 2f -> q
                tt < 2f / 3f -> p + (q - p) * (2f / 3f - tt) * 6f
                else -> p
            }
        }
    }
}
