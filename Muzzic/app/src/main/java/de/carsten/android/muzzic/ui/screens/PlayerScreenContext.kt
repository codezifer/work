package de.carsten.android.muzzic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.model.AlbumArtInput
import de.carsten.android.muzzic.ui.screens.controls.AlbumArtControl
import de.carsten.android.muzzic.ui.screens.controls.VolumeControl
import de.carsten.android.muzzic.ui.theme.AppTheme
import de.carsten.android.muzzic.ui.utils.formatDuration
import de.carsten.android.muzzic.utils.UI_EMPTY

@Composable
fun PlayerScreenContext(
    albumArtPath: String?,
    songTitle: String?,
    artistName: String?,
    isPlaying: Boolean,
    progress: Float, // Value between 0f and 1f
    duration: Long,
    onPlayPauseClicked: () -> Unit = {},
    onNextClicked: () -> Unit = {},
    onPreviousClicked: () -> Unit = {},
    onProgressChanged: (Float) -> Unit = {}, // Callback for when user scrubs the progress bar
) {
    AppTheme {
        val textColor = MaterialTheme.colorScheme.onSurface
        val leftDuration: String = formatDuration((duration * progress).toLong())
        val rightDuration: String = formatDuration((duration - (duration * progress)).toLong())

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Album Cover
            AlbumArtControl(
                if (albumArtPath == null) {
                    AlbumArtInput.None
                } else {
                    AlbumArtInput.FromPath(albumArtPath)
                },
            )
            Spacer(modifier = Modifier.height(24.dp))

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

            Spacer(modifier = Modifier.height(24.dp))

            // Progress Bar
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = progress,
                    onValueChange = onProgressChanged,
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                    SliderDefaults.colors(
                        thumbColor = textColor,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = textColor.copy(alpha = 0.3f),
                    ),
                )
                Row(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    // Align with slider padding
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    // You'd replace these with actual formatted time
                    Text(
                        text = leftDuration,
                        color = textColor.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                    )
                    Text(
                        rightDuration,
                        color = textColor.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

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
                            ).background(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                shape = CircleShape,
                            )
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
                                MaterialTheme.colorScheme.primary,
                                CircleShape,
                            ),
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
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
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // volume
            VolumeControl()
        }
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
    )
}
