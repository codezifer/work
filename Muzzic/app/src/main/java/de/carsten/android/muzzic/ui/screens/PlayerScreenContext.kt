package de.carsten.android.muzzic.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.ui.theme.AppTheme
import de.carsten.android.muzzic.ui.utils.formatDuration
import de.carsten.android.muzzic.utils.uiEmpty

@Composable
fun PlayerScreenContext(
    albumArtPainter: Painter,
    songTitle: String?,
    artistName: String?,
    isPlaying: Boolean,
    progress: Float, // Value between 0f and 1f
    duration: Long,
    onPlayPauseClicked: () -> Unit,
    onNextClicked: () -> Unit,
    onPreviousClicked: () -> Unit,
    onProgressChanged: (Float) -> Unit, // Callback for when user scrubs the progress bar
) {
    AppTheme {
        val discResource = painterResource(R.drawable.disc)
        val imageModifier =
            Modifier
                .fillMaxWidth(0.8f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.DarkGray)

        val textColor = MaterialTheme.colorScheme.onSurface

        val getDiscImage = @Composable {
            Image(
                painter = discResource,
                contentDescription = "On Error or Empty state, disc image",
                modifier = imageModifier,
                contentScale = ContentScale.Crop,
            )
        }
        val getStateImage = @Composable {
            when (albumArtPainter) {
                is AsyncImagePainter -> {
                    when (albumArtPainter.state) {
                        is AsyncImagePainter.State.Loading -> {
                            CircularProgressIndicator(
                                modifier =
                                    Modifier
                                        .fillMaxWidth(0.8f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(100)),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 22.dp,
                            )
                        }

                        is AsyncImagePainter.State.Empty,
                        is AsyncImagePainter.State.Error,
                        -> {
                            getDiscImage()
                        }

                        is AsyncImagePainter.State.Success -> {
                            AsyncImage(
                                model = albumArtPainter,
                                contentDescription = "Album Art",
                                modifier =
                                    Modifier
                                        .fillMaxWidth(0.8f)
                                        .aspectRatio(1.0f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.DarkGray),
                                contentScale = ContentScale.Crop,
                                placeholder = discResource,
                                error = discResource,
                            )
                        }
                    }
                }

                else -> {
                    getDiscImage()
                }
            }
        }

        val leftDuration: String = formatDuration((duration * progress).toLong())
        val rightDuration: String = formatDuration((duration - (duration * progress)).toLong())

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Album Cover
            getStateImage()
            Spacer(modifier = Modifier.height(24.dp))

            // Song Info
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = songTitle ?: uiEmpty,
                    color = textColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = artistName ?: uiEmpty,
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // prev-button
                IconButton(onClick = onPreviousClicked) {
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
                IconButton(onClick = onNextClicked) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Next Track",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
        }
    }
}

@Composable
@Preview
@Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
fun PlayerScreenPreview_Playing() {
    // In a real app, you'd get this from a ViewModel or Coil/Glide
    // For preview, using a placeholder icon if you don't have R.drawable.album_art_placeholder
    val albumArtPainter =
        painterResource(id = R.drawable.disc) // Replace with your actual placeholder

    MaterialTheme {
        // Ensure MaterialTheme is applied for default styles
        PlayerScreenContext(
            albumArtPainter = albumArtPainter,
            songTitle = "The Greatest Show",
            artistName = "Panic! At The Disco",
            isPlaying = true,
            progress = 0.45f,
            duration = 225000,
            onPlayPauseClicked = {},
            onNextClicked = {},
            onPreviousClicked = {},
            onProgressChanged = {},
        )
    }
}

@Composable
@Preview
@Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
fun PlayerScreenPreview_Paused() {
    val albumArtPainter = painterResource(id = R.drawable.disc)

    MaterialTheme {
        PlayerScreenContext(
            albumArtPainter = albumArtPainter,
            songTitle = "Bohemian Rhapsody (Remastered 2011)",
            artistName = "Queen",
            isPlaying = false,
            progress = 0.15f,
            duration = 225000,
            onPlayPauseClicked = {},
            onNextClicked = {},
            onPreviousClicked = {},
            onProgressChanged = {},
        )
    }
}

@Composable
@Preview
@Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
fun PlayerScreenPreview_Loading() {
    val loadingPainter =
        rememberAsyncImagePainter(
            model = null,
            onState = { AsyncImagePainter.State.Loading(null) },
        )

    MaterialTheme {
        PlayerScreenContext(
            albumArtPainter = loadingPainter,
            songTitle = "Bohemian Rhapsody (Remastered 2011)",
            artistName = "Queen",
            isPlaying = false,
            progress = 0.15f,
            duration = 225000,
            onPlayPauseClicked = {},
            onNextClicked = {},
            onPreviousClicked = {},
            onProgressChanged = {},
        )
    }
}
