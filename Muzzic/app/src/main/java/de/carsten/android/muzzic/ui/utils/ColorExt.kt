package de.carsten.android.muzzic.ui.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

private const val DARK_LUMINANCE_FACTOR = 0.4f
private const val LIGHT_LUMINANCE_FACTOR = 0.6f
private const val MAX_LUMINANCE = 1f
private const val DARKEN_FACTOR = 0.7f

/**
 * Adjusts a color to be more suitable for the current theme.
 *
 * In Dark Theme, if the color is too dark (luminance < 0.4), it is lightened.
 * In Light Theme, if the color is too light (luminance > 0.6), it is darkened.
 *
 * @param isDark Whether the current theme is Dark Theme.
 * @return An adjusted [Color] that ensures better visibility.
 */
fun Color.adjustForTheme(isDark: Boolean): Color {
    val lum = luminance()
    return when {
        isDark && lum < DARK_LUMINANCE_FACTOR -> {
            // Lighten the color in dark theme if it's too dark
            this.copy(
                red = (red + DARK_LUMINANCE_FACTOR).coerceAtMost(MAX_LUMINANCE),
                green = (green + DARK_LUMINANCE_FACTOR).coerceAtMost(MAX_LUMINANCE),
                blue = (blue + DARK_LUMINANCE_FACTOR).coerceAtMost(MAX_LUMINANCE),
            )
        }

        !isDark && lum > LIGHT_LUMINANCE_FACTOR -> {
            // Darken the color in light theme if it's too light
            this.copy(
                red = (red * DARKEN_FACTOR),
                green = (green * DARKEN_FACTOR),
                blue = (blue * DARKEN_FACTOR),
            )
        }

        else -> this
    }
}

/**
 * Returns a high-contrast content color (Black or White) based on the background luminance.
 *
 * @return [Color.Black] if the background is light, [Color.White] if it is dark.
 */
fun Color.contrastColor(): Color = if (luminance() > 0.45f) Color.Black else Color.White

/**
 * Calculates the contrast ratio between two colors according to WCAG standards.
 * Formula: (L1 + 0.05) / (L2 + 0.05) where L is relative luminance.
 *
 * @param other The other color to compare against.
 * @return The contrast ratio as a [Float] between 1.0 and 21.0.
 */
fun Color.calculateContrast(other: Color): Float {
    val l1 = luminance() + 0.05f
    val l2 = other.luminance() + 0.05f
    return if (l1 > l2) l1 / l2 else l2 / l1
}

/**
 * Ensures this color has sufficient contrast against the [backgroundColor].
 * If the contrast ratio is below [minContrast], returns a high-contrast fallback (Black/White).
 *
 * @param backgroundColor The background color this color will be displayed on.
 * @param minContrast The minimum required contrast ratio (default is 3.0 for large text).
 * @return This color if contrast is sufficient, otherwise a high-contrast fallback.
 */
fun Color.ensureContrast(backgroundColor: Color, minContrast: Float = 3.0f): Color {
    return if (calculateContrast(backgroundColor) < minContrast) {
        backgroundColor.contrastColor()
    } else {
        this
    }
}
