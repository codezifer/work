package de.carsten.android.muzzic.ui.screens.controls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import de.carsten.android.muzzic.ui.BLUR_RADIUS_LARGE
import de.carsten.android.muzzic.ui.BORDER_WIDTH_THICK
import de.carsten.android.muzzic.ui.ICON_SIZE_EXTRA_LARGE
import de.carsten.android.muzzic.ui.ICON_SIZE_PLAY_BUTTON_LARGE
import de.carsten.android.muzzic.ui.SPACING_SMALL
import de.carsten.android.muzzic.ui.theme.AppTheme

/**
 * A custom play button with a blurry, translucent background and a stylish border.
 *
 * In Dark Theme, it has a "milky" blurry look (white with alpha).
 * In Light Theme, it has a "dark" blurry look (black with alpha).
 *
 * @param onClick Callback when the button is clicked.
 * @param modifier Modifier for the button.
 * @param borderColor The color of the border, typically the accent color.
 * @param size The size of the button.
 * @param blur The blur radius for the background.
 */
@Composable
fun PlayButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.primary,
    size: Dp = ICON_SIZE_EXTRA_LARGE,
    blur: Dp = BLUR_RADIUS_LARGE,
) {
    val isDark = isSystemInDarkTheme()

    // Background color based on theme for the "blurry" effect
    // Milky white for dark theme, dark grey for light theme
    val blurColor = if (isDark) {
        Color.White.copy(alpha = 0.25f)
    } else {
        Color.Black.copy(alpha = 0.35f)
    }

    // Icon color: usually primary or white/black depending on contrast
    val iconColor = if (isDark) {
        Color.White
    } else {
        Color.White // Keep it white for better contrast on dark blur
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .drawBehind {
                // Outer Glow effect
                drawCircle(
                    color = borderColor.copy(alpha = 0.6f),
                    radius = (size.toPx() / 2f) + SPACING_SMALL.toPx(),
                    alpha = 0.4f,
                )
            }
            .border(BorderStroke(BORDER_WIDTH_THICK, borderColor), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        // Blurry Background Layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .blur(blur)
                .background(blurColor),
        )

        // Button Layer
        IconButton(
            onClick = onClick,
            modifier = Modifier.matchParentSize(),
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = iconColor,
                modifier = Modifier.size(size.times(0.75f)),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlayButtonLightPreview() {
    AppTheme {
        Box(
            modifier = Modifier
                .size(ICON_SIZE_PLAY_BUTTON_LARGE)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            PlayButton(onClick = {})
        }
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PlayButtonDarkPreview() {
    AppTheme {
        Box(
            modifier = Modifier
                .size(ICON_SIZE_PLAY_BUTTON_LARGE)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            PlayButton(onClick = {})
        }
    }
}
