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
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.ui.backgroundColor
import de.carsten.android.muzzic.ui.primaryColor


// Assuming you have these colors defined, or replace with your actual colors
val DarkPlayerBackground = backgroundColor
val LightPlayerText = Color.White
val PlayerAccentColor = primaryColor

@Composable
fun PlayerScreenContent(
    albumArtPainter: Painter,
    songTitle: String,
    artistName: String,
    isPlaying: Boolean,
    progress: Float, // Value between 0f and 1f
    onPlayPauseClicked: () -> Unit,
    onNextClicked: () -> Unit,
    onPreviousClicked: () -> Unit,
    onProgressChanged: (Float) -> Unit // Callback for when user scrubs the progress bar

) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkPlayerBackground)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Album Cover
        Image(
            painter = albumArtPainter,
            contentDescription = "Album Art",
            modifier = Modifier
                .fillMaxWidth(0.8f) // Take 80% of width
                .aspectRatio(1f) // Square
                .clip(RoundedCornerShape(12.dp))
                .background(Color.DarkGray), // Placeholder background if image is transparent
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Song Info
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = songTitle,
                color = LightPlayerText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = artistName,
                color = LightPlayerText.copy(alpha = 0.7f),
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Progress Bar
        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = progress,
                onValueChange = onProgressChanged,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = LightPlayerText,
                    activeTrackColor = PlayerAccentColor,
                    inactiveTrackColor = LightPlayerText.copy(alpha = 0.3f)
                )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp), // Align with slider padding
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // You'd replace these with actual formatted time
                Text("0:00", color = LightPlayerText.copy(alpha = 0.7f), fontSize = 12.sp)
                Text("3:45", color = LightPlayerText.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }


        Spacer(modifier = Modifier.height(24.dp))

        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousClicked) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "Previous Track",
                    tint = LightPlayerText,
                    modifier = Modifier.size(40.dp)
                )
            }

            IconButton(
                onClick = onPlayPauseClicked,
                modifier = Modifier
                    .size(72.dp) // Larger play/pause button
                    .background(PlayerAccentColor, CircleShape)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = DarkPlayerBackground, // Icon color contrasts with accent background
                    modifier = Modifier.size(44.dp)
                )
            }

            IconButton(onClick = onNextClicked) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next Track",
                    tint = LightPlayerText,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000 /* Black for preview */)
@Composable
fun PlayerScreenPreview_Playing() {
    // In a real app, you'd get this from a ViewModel or Coil/Glide
    // For preview, using a placeholder icon if you don't have R.drawable.album_art_placeholder
    val albumArtPainter =
        painterResource(id = R.drawable.disc) // Replace with your actual placeholder

    MaterialTheme { // Ensure MaterialTheme is applied for default styles
        PlayerScreenContent(
            albumArtPainter = albumArtPainter,
            songTitle = "The Greatest Show",
            artistName = "Panic! At The Disco",
            isPlaying = true,
            progress = 0.45f,
            onPlayPauseClicked = {},
            onNextClicked = {},
            onPreviousClicked = {},
            onProgressChanged = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun PlayerScreenPreview_Paused() {
    val albumArtPainter = painterResource(id = R.drawable.disc)

    MaterialTheme {
        PlayerScreenContent(
            albumArtPainter = albumArtPainter,
            songTitle = "Bohemian Rhapsody (Remastered 2011)",
            artistName = "Queen",
            isPlaying = false,
            progress = 0.15f,
            onPlayPauseClicked = {},
            onNextClicked = {},
            onPreviousClicked = {},
            onProgressChanged = {}
        )
    }
}
