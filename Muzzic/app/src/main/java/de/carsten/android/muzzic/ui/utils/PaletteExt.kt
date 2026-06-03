package de.carsten.android.muzzic.ui.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette

/**
 * Data class to hold background and matching content colors extracted from a Palette.
 *
 * @property backgroundColor The main color to be used as background.
 * @property contentColor A color that is readable against the background.
 */
data class PaletteColors(val backgroundColor: Color, val contentColor: Color)

/**
 * Extracts a representative color pair from the [Palette].
 *
 * Prioritizes vibrant swatches over dominant ones to better capture the visual character
 * of album art. Handles the conversion from Android color ints to Compose [Color] objects.
 *
 * @param defaultBackground The color to return if no suitable swatch is found.
 * @param defaultContent The content color to return if no suitable swatch is found.
 * @return A [PaletteColors] object containing the background and content colors.
 */
fun Palette?.extractColors(defaultBackground: Color, defaultContent: Color): PaletteColors {
    if (this == null) return PaletteColors(defaultBackground, defaultContent)

    // Prioritize vibrant colors as they usually represent the "character" of the art better
    // than the dominant color (which is often just a background or border).
    val swatch = vibrantSwatch
        ?: lightVibrantSwatch
        ?: darkVibrantSwatch
        ?: dominantSwatch
        ?: mutedSwatch

    return if (swatch != null) {
        PaletteColors(
            backgroundColor = Color(swatch.rgb),
            contentColor = Color(swatch.bodyTextColor),
        )
    } else {
        // Fallback using getDominantColor with proper ARGB conversion
        val dominantRgb = getDominantColor(defaultBackground.toArgb())
        PaletteColors(
            backgroundColor = Color(dominantRgb),
            contentColor = defaultContent,
        )
    }
}
