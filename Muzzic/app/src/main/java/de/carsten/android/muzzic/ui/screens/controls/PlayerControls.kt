package de.carsten.android.muzzic.ui.screens.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.ui.BORDER_WIDTH_FAT
import de.carsten.android.muzzic.ui.BORDER_WIDTH_NORMAL
import de.carsten.android.muzzic.ui.BORDER_WIDTH_THICK
import de.carsten.android.muzzic.ui.ICON_SIZE_LARGE
import de.carsten.android.muzzic.ui.ICON_SIZE_MEDIUM
import de.carsten.android.muzzic.ui.ICON_SIZE_PLAYER_MAIN
import de.carsten.android.muzzic.ui.ICON_SIZE_PLAYER_PLAY_PAUSE
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.SPACING_MEDIUM
import de.carsten.android.muzzic.ui.component.MusicVisualization
import de.carsten.android.muzzic.ui.model.ColorSource
import de.carsten.android.muzzic.ui.model.composableColorSource
import de.carsten.android.muzzic.ui.theme.AppTheme

/**
 * A dedicated component for player controls with integrated music visualization background.
 */
@Composable
fun PlayerControls(
    isPlaying: Boolean,
    shuffleModeEnabled: Boolean,
    repeatMode: Int,
    amplitudesProvider: () -> List<Float>,
    colorSource: ColorSource,
    onPlayPauseClicked: () -> Unit,
    onNextClicked: () -> Unit,
    onPreviousClicked: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(CircleShape)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = CircleShape,
            )
            .border(
                width = BORDER_WIDTH_THICK,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Background Visualization
        MusicVisualization(
            amplitudesProvider = amplitudesProvider,
            isPlaying = isPlaying,
            color = colorSource.accentColor.copy(alpha = 0.4f),
            modifier = Modifier.matchParentSize(),
        )

        // Control Buttons
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            val circularButtonModifier =
                Modifier
                    .size(ICON_SIZE_LARGE)
                    .border(
                        width = BORDER_WIDTH_NORMAL,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        shape = CircleShape,
                    )
                    .background(
                        color = colorSource.accentColor.copy(alpha = 0.8f),
                        shape = CircleShape,
                    )

            // shuffle-button
            IconButton(
                onClick = onToggleShuffle,
                modifier = Modifier.circleBorder(colorSource.accentColor, shuffleModeEnabled),
            ) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = stringResource(R.string.shuffle),
                    tint = if (shuffleModeEnabled) colorSource.accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(ICON_SIZE_MEDIUM),
                )
            }

            // prev-button
            IconButton(
                onClick = onPreviousClicked,
                modifier = circularButtonModifier,
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = stringResource(R.string.previous),
                    tint = colorSource.contentColor,
                    modifier = Modifier.size(ICON_SIZE_LARGE),
                )
            }

            // play-pause-button
            IconButton(
                onClick = onPlayPauseClicked,
                modifier =
                Modifier
                    .size(ICON_SIZE_PLAYER_MAIN)
                    .background(
                        colorSource.accentColor.copy(alpha = 0.8f),
                        CircleShape,
                    ),
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
                    tint = colorSource.contentColor,
                    modifier = Modifier.size(ICON_SIZE_PLAYER_PLAY_PAUSE),
                )
            }

            // next-button
            IconButton(
                onClick = onNextClicked,
                modifier = circularButtonModifier,
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = stringResource(R.string.next),
                    tint = colorSource.contentColor,
                    modifier = Modifier.size(ICON_SIZE_LARGE),
                )
            }

            // repeat-button
            IconButton(
                onClick = onToggleRepeat,
                modifier = Modifier.circleBorder(colorSource.accentColor, repeatMode != Player.REPEAT_MODE_OFF),
            ) {
                val repeatIcon = when (repeatMode) {
                    Player.REPEAT_MODE_ONE -> Icons.Filled.RepeatOne
                    else -> Icons.Filled.Repeat
                }
                Icon(
                    imageVector = repeatIcon,
                    contentDescription = stringResource(R.string.repeat),
                    tint = if (repeatMode != Player.REPEAT_MODE_OFF) colorSource.accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(ICON_SIZE_MEDIUM),
                )
            }
        }
    }
}

private fun Modifier.circleBorder(color: Color, enabling: Boolean): Modifier {
    val modifier = this.size(ICON_SIZE_LARGE)
    return if (enabling) {
        modifier.border(
            width = BORDER_WIDTH_FAT,
            color = color,
            shape = CircleShape,
        )
    } else {
        modifier
    }
}

@Composable
@Preview(showBackground = true, name = "Light Mode")
@Preview(uiMode = PREVIEW_DARK_MODE, showBackground = true, name = "Dark Mode")
fun PlayerControlsPreview() {
    AppTheme {
        Box(modifier = Modifier.padding(SPACING_MEDIUM)) {
            PlayerControls(
                isPlaying = true,
                shuffleModeEnabled = true,
                repeatMode = Player.REPEAT_MODE_ALL,
                amplitudesProvider = { listOf(0.2f, 0.5f, 0.8f, 0.4f, 0.6f, 0.9f, 0.3f, 0.7f, 0.5f, 0.2f, 0.8f, 0.4f, 0.6f, 0.9f, 0.3f, 0.7f) },
                colorSource = composableColorSource(),
                onPlayPauseClicked = {},
                onNextClicked = {},
                onPreviousClicked = {},
                onToggleShuffle = {},
                onToggleRepeat = {},
            )
        }
    }
}
