package de.carsten.android.muzzic.ui.utils

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import de.carsten.android.muzzic.model.toAlbumArtUri
import de.carsten.android.muzzic.ui.model.ColorSource
import de.carsten.android.muzzic.ui.model.composableColorSource
import de.carsten.android.muzzic.ui.screens.cards.rememberPaletteState

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

    // Strictly prioritize vibrant colors to better capture the visual character of album art.
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

/**
 * Data class to hold adaptive content colors calculated based on a background.
 */
@Immutable
data class AdaptiveContentColors(val contentColor: Color, val labelColor: Color, val accentColor: Color, val overlayColor: Color)

/**
 * Remembers adaptive content colors based on an album art and an overlay color.
 *
 * @param albumArt The album art data.
 * @param overlayColor The overlay color to be applied over the album art.
 * @param colorSource The source for accent, content and label colors.
 * @param isCurrent Whether the item is the current song (influences default color selection).
 * @return An [AdaptiveContentColors] object containing the calculated colors.
 */
@Composable
fun rememberAdaptiveContentColors(albumArt: String?, overlayColor: Color, colorSource: ColorSource = composableColorSource(), isCurrent: Boolean = false): AdaptiveContentColors {
    val palette by rememberPaletteState(albumArt.toAlbumArtUri())
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    return remember(palette, overlayColor, colorSource, isCurrent, onSurface, onSurfaceVariant) {
        val dominantColor = palette?.getDominantColor(Color.Transparent.toArgb())?.let { Color(it) }
            ?: Color.Transparent

        val effectiveBackground = overlayColor.compositeOver(dominantColor)

        AdaptiveContentColors(
            contentColor = (if (isCurrent) colorSource.contentColor else onSurface)
                .ensureContrast(effectiveBackground),
            labelColor = (if (isCurrent) colorSource.labelColor else onSurfaceVariant)
                .ensureContrast(effectiveBackground),
            accentColor = colorSource.accentColor.ensureContrast(effectiveBackground),
            overlayColor = overlayColor,
        )
    }
}
