package de.carsten.android.muzzic.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import de.carsten.android.muzzic.ui.BACKGROUND_OVERLAY_ALPHA
import de.carsten.android.muzzic.ui.theme.CustomColors

/**
 * Renders a blurred background using the album art of the current song.
 *
 * @param albumArtPath The path to the album art image.
 * @param blurRadius The radius of the blur effect.
 */
@Composable
fun PlayerBackground(albumArtPath: String?, blurRadius: Dp) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Base Gradient (fallback if no image is available)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            CustomColors.gradient1,
                            CustomColors.gradient2,
                            CustomColors.gradient3,
                        ),
                    ),
                ),
        )

        // Blurred Album Art
        AnimatedContent(
            targetState = albumArtPath,
            transitionSpec = {
                fadeIn(animationSpec = tween(500)).togetherWith(fadeOut(animationSpec = tween(500)))
            },
            label = "PlayerBackgroundTransition",
        ) { targetPath ->
            if (targetPath != null) {
                AsyncImage(
                    model = targetPath,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(blurRadius),
                )
            } else {
                // Return an empty box if no album art is available
                Box(modifier = Modifier.fillMaxSize())
            }
        }

        // Dark Overlay to maintain contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = BACKGROUND_OVERLAY_ALPHA)),
        )
    }
}
