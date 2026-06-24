package de.carsten.android.muzzic.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import de.carsten.android.muzzic.ui.GLASS_CONTAINER_ALPHA
import de.carsten.android.muzzic.ui.GLASS_PANEL_CORNER_RADIUS
import de.carsten.android.muzzic.ui.SPACING_LARGE

/**
 * A reusable glass-like container that provides a semi-transparent surface
 * to ensure text readability over the blurred background.
 */
@Composable
fun GlassContainer(modifier: Modifier = Modifier, cornerRadius: Dp = GLASS_PANEL_CORNER_RADIUS, alpha: Float = GLASS_CONTAINER_ALPHA, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = alpha),
                shape = RoundedCornerShape(cornerRadius),
            )
            .padding(SPACING_LARGE),
    ) {
        content()
    }
}
