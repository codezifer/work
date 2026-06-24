package de.carsten.android.muzzic.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import de.carsten.android.muzzic.ui.BLUR_RADIUS_LARGE
import de.carsten.android.muzzic.ui.BORDER_WIDTH_THIN
import de.carsten.android.muzzic.ui.ELEVATION_LARGE
import de.carsten.android.muzzic.ui.ELEVATION_SMALL
import de.carsten.android.muzzic.ui.GLASS_PANEL_CORNER_RADIUS

/**
 * A reusable container that provides a "glassmorphism" effect.
 *
 * It features a semi-transparent background, a subtle border for definition,
 * and elevation to create a floating appearance.
 *
 * @param modifier The modifier to be applied to the panel.
 * @param containerColor The base color of the panel (alpha will be applied).
 * @param alpha The transparency level of the background.
 * @param shape The shape of the panel.
 * @param borderAlpha The transparency level of the white border.
 * @param content The content to be placed inside the panel.
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    alpha: Float = 0.65f,
    shape: Shape = RoundedCornerShape(GLASS_PANEL_CORNER_RADIUS),
    borderAlpha: Float = 0.2f,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        color = Color.Transparent,
        shape = shape,
        border = BorderStroke(BORDER_WIDTH_THIN, Color.White.copy(alpha = borderAlpha)),
        shadowElevation = ELEVATION_LARGE,
        tonalElevation = ELEVATION_SMALL,
    ) {
        Box {
            // Background layer with blur
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(BLUR_RADIUS_LARGE)
                    .background(containerColor.copy(alpha = alpha)),
            )

            // Content layer
            Box {
                content()
            }
        }
    }
}
