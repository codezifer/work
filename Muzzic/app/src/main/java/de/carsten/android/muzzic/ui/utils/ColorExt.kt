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
