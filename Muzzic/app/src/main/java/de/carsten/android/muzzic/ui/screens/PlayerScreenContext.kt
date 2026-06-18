package de.carsten.android.muzzic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.component.IndicatorSlider
import de.carsten.android.muzzic.ui.model.AlbumArtInput
import de.carsten.android.muzzic.ui.model.ColorSource
import de.carsten.android.muzzic.ui.screens.controls.AlbumArtControl
import de.carsten.android.muzzic.ui.screens.controls.VolumeControl
import de.carsten.android.muzzic.ui.utils.formatDuration
import de.carsten.android.muzzic.utils.UI_EMPTY

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreenContext(
    albumArtPath: String?,
    songTitle: String?,
    artistName: String?,
    isPlaying: Boolean,
    progress: Float, // Value between 0f and 1f
    duration: Long,
    shuffleModeEnabled: Boolean = false,
    repeatMode: Int = Player.REPEAT_MODE_OFF,
    colorSource: ColorSource = ColorSource(accentColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.inversePrimary),
    onPlayPauseClicked: () -> Unit = {},
    onNextClicked: () -> Unit = {},
    onPreviousClicked: () -> Unit = {},
    onProgressChanged: (Float) -> Unit = {}, // Callback for when user scrubs the progress bar
    onToggleShuffle: () -> Unit = {},
    onToggleRepeat: () -> Unit = {},
) {
    val textColor = MaterialTheme.colorScheme.onSurface

    // We don't need local scrubbing state anymore as IndicatorSlider handles it
    // But we need it for the duration text below the slider if we want those to update too
    // Let's keep a simplified version or just accept that those labels only update on finish
    // User asked for "numerische Position" to be displayed, which we do in the bubble.

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        // Album Cover
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            AlbumArtControl(
                albumArtInput = if (albumArtPath == null) {
                    AlbumArtInput.None
                } else {
                    AlbumArtInput.FromPath(albumArtPath)
                },
            )
        }

        // Song Info
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = songTitle ?: UI_EMPTY,
                color = textColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = artistName ?: UI_EMPTY,
                color = textColor.copy(alpha = 0.7f),
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Progress Bar
        Column(modifier = Modifier.fillMaxWidth()) {
            IndicatorSlider(
                value = progress,
                onValueChange = { /* handled by indicator internal state */ },
                onValueChangeFinished = { newVal ->
                    onProgressChanged(newVal)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = colorSource.accentColor,
                    activeTrackColor = colorSource.accentColor,
                    inactiveTrackColor = textColor.copy(alpha = 0.3f),
                ),
                indicatorFormatter = { valPos -> formatDuration((duration * valPos).toLong()) },
                indicatorColor = colorSource.accentColor,
            )
            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                // Align with slider padding
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formatDuration((duration * progress).toLong()),
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                )
                Text(
                    text = formatDuration((duration - (duration * progress)).toLong()),
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                )
            }
        }

        // Controls
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier =
            Modifier
                .fillMaxWidth()
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    shape = CircleShape,
                ),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = 8.dp,
                    ),
            ) {
                val circularButtonModifier =
                    Modifier
                        .size(40.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            shape = CircleShape,
                        )
                        .background(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            shape = CircleShape,
                        )

                // shuffle-button
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (shuffleModeEnabled) colorSource.accentColor else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp),
                    )
                }

                // prev-button
                IconButton(
                    onClick = onPreviousClicked,
                    modifier = circularButtonModifier,
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Previous Track",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(40.dp),
                    )
                }

                // play-pause-button
                IconButton(
                    onClick = onPlayPauseClicked,
                    modifier =
                    Modifier
                        .size(72.dp) // Larger play/pause button
                        .background(
                            colorSource.accentColor,
                            CircleShape,
                        ),
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = colorSource.contentColor,
                        modifier = Modifier.size(44.dp),
                    )
                }

                // next-button
                IconButton(
                    onClick = onNextClicked,
                    modifier = circularButtonModifier,
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Next Track",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(40.dp),
                    )
                }

                // repeat-button
                IconButton(
                    onClick = onToggleRepeat,
                    modifier = Modifier.size(40.dp),
                ) {
                    val repeatIcon = when (repeatMode) {
                        Player.REPEAT_MODE_ONE -> Icons.Filled.RepeatOne
                        else -> Icons.Filled.Repeat
                    }
                    Icon(
                        imageVector = repeatIcon,
                        contentDescription = "Repeat",
                        tint = if (repeatMode != Player.REPEAT_MODE_OFF) colorSource.accentColor else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }

        // volume
        VolumeControl(accentColor = colorSource.accentColor)
    }
}

@Composable
@Preview
@Preview(uiMode = PREVIEW_DARK_MODE, name = "PlayerScreenPreview_Playing_Dark")
fun PlayerScreenPreview_Playing() {
    PlayerScreenContext(
        albumArtPath = null,
        songTitle = "The Greatest Show",
        artistName = "Panic! At The Disco",
        isPlaying = true,
        progress = 0.45f,
        duration = 225000,
        shuffleModeEnabled = true,
        repeatMode = Player.REPEAT_MODE_ALL,
    )
}

@Composable
@Preview
@Preview(uiMode = PREVIEW_DARK_MODE, name = "PlayerScreenPreview_Paused_Dark")
fun PlayerScreenPreview_Paused() {
    PlayerScreenContext(
        albumArtPath = null,
        songTitle = "Bohemian Rhapsody (Remastered 2011)",
        artistName = "Queen",
        isPlaying = false,
        progress = 0.15f,
        duration = 225000,
        shuffleModeEnabled = false,
        repeatMode = Player.REPEAT_MODE_OFF,
    )
}
