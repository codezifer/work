package de.carsten.android.muzzic.ui.screens.cards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.carsten.android.muzzic.persistence.entity.Song
import de.carsten.android.muzzic.ui.containerColor
import de.carsten.android.muzzic.ui.utils.EMPTY
import de.carsten.android.muzzic.ui.utils.formatDuration
import java.time.Instant

@Composable
fun SongListItem(song: Song) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { /* Play song */ },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Song Icon
            Card(
                modifier = Modifier.size(40.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = containerColor,
                    ),
                shape = RoundedCornerShape(8.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Song Info
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = song.title ?: EMPTY,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = "${song.artist} • ${song.album}",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Rating and Duration
            Column(
                horizontalAlignment = Alignment.End,
            ) {
                StarRating(
                    rating = song.rating ?: 0,
                    onRatingChanged = { /* Update rating */ },
                    size = 12.dp,
                )

                Text(
                    text = formatDuration(song.duration ?: 0),
                    color = Color.Gray,
                    fontSize = 10.sp,
                )
            }
        }
    }
}

@Composable
@Preview
fun SongListItemPreview() {
    SongListItem(
        Song(
            title = "This is just a Test",
            album = "Test-Album",
            artist = "Test-Artist",
            duration = 3 * 60 * 1000,
            genre = "Alternative",
            lastPlayed = Instant.now(),
            playCount = 3,
            rating = 3,
            totalTracks = 10,
            trackNumber = 3,
        ),
    )
}
