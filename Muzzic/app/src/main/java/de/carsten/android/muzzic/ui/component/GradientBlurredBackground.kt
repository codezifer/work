package de.carsten.android.muzzic.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.ui.BLUR_RADIUS_LARGE
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.theme.AppTheme

/**
 * A background component that renders an image with a gradient blur effect.
 * The blur is strongest on the left and gradually clears towards the right.
 *
 * @param data The data to load (e.g., URL, URI, or File).
 * @param modifier The modifier to be applied to the background.
 * @param blurRadius The maximum blur radius applied on the left.
 * @param contentScale How the image should be scaled.
 */
@Composable
fun GradientBlurredBackground(data: Any?, modifier: Modifier = Modifier, blurRadius: Dp = BLUR_RADIUS_LARGE, contentScale: ContentScale = ContentScale.Crop) {
    Box(modifier = modifier) {
        // 1. Fully Blurred Background Layer
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(data)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .matchParentSize()
                .blur(blurRadius),
            contentScale = contentScale,
            placeholder = painterResource(R.drawable.disc),
            error = painterResource(R.drawable.disc),
        )

        // 2. Clear Foreground Layer with Horizontal Gradient Mask
        // We use BlendMode.DstIn to mask the clear image with an alpha gradient.
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(data)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent, // Blurred part (left)
                                Color.Black, // Clear part (right)
                            ),
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                },
            contentScale = contentScale,
        )
    }
}

@Composable
@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = PREVIEW_DARK_MODE, showBackground = true)
fun GradientBlurredBackgroundPreview() {
    AppTheme {
        GradientBlurredBackground(
            data = R.drawable.disc,
            modifier = Modifier.size(300.dp, 100.dp),
        )
    }
}
