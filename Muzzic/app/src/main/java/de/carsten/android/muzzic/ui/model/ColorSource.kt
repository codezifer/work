package de.carsten.android.muzzic.ui.model

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import de.carsten.android.muzzic.ui.theme.CustomColors

/**
 * A data class representing a set of related colors used for UI components.
 *
 * @property accentColor The primary background or accent color.
 * @property contentColor The primary color for icons and high-emphasis text.
 * @property labelColor The color for secondary text or labels, typically with lower emphasis.
 * @property neutralColor The color for neutral marking
 * @property onNeutralColor The text color for neutral marking
 */
@Immutable
data class ColorSource(val accentColor: Color, val contentColor: Color, val labelColor: Color = contentColor.copy(alpha = 0.7f), val neutralColor: Color, val onNeutralColor: Color)

@Composable
@ReadOnlyComposable
fun composableColorSource(
    accentColor: Color? = null,
    contentColor: Color? = null,
    labelColor: Color? = null,
    neutralColor: Color? = null,
    onNeutralColor: Color? = null,
): ColorSource = ColorSource(
    accentColor = accentColor ?: MaterialTheme.colorScheme.primary,
    contentColor = contentColor ?: MaterialTheme.colorScheme.onPrimary,
    labelColor = (labelColor ?: (contentColor ?: MaterialTheme.colorScheme.onPrimary)).copy(alpha = 0.7f),
    neutralColor = neutralColor ?: CustomColors.neutral,
    onNeutralColor = onNeutralColor ?: CustomColors.onNeutral,
)
