package de.carsten.android.muzzic.ui.model

import androidx.compose.ui.graphics.Color

/**
 * A data class representing a set of related colors used for UI components.
 *
 * @property accentColor The primary background or accent color.
 * @property contentColor The primary color for icons and high-emphasis text.
 * @property labelColor The color for secondary text or labels, typically with lower emphasis.
 */
data class ColorSource(val accentColor: Color, val contentColor: Color, val labelColor: Color = contentColor.copy(alpha = 0.7f))
