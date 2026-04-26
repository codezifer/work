package de.carsten.android.muzzic.ui.screens.cards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.ui.MAINTITLE_FONTSIZE
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.SUBTITLE_FONTSIZE
import de.carsten.android.muzzic.ui.model.AlbumDto
import de.carsten.android.muzzic.ui.theme.AppTheme

@Composable
fun AlbumCard(
    album: AlbumDto,
    grid: Boolean = false,
    onClick: () -> Unit = {},
) {
    AppTheme {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clickable(onClick = onClick),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(12.dp),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Album Cover
                Card(
                    modifier = Modifier.size(120.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(
                            model = album.lastAlbumArt,
                            contentDescription = "AlbumArt",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(R.drawable.disc),
                            error = painterResource(R.drawable.disc),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = album.albumName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = MAINTITLE_FONTSIZE,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                )

                if (!grid) {
                    SubtitleInformation(
                        listOf(
                            Pair(Icons.Default.Person, album.artistName),
                            Pair(Icons.Default.MusicNote, "${album.songCount} Songs"),
                            Pair(Icons.Default.CalendarMonth, album.albumYear.toString()),
                        ),
                        fontColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                } else {
                    Text(
                        text = album.artistName,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = SUBTITLE_FONTSIZE,
                    )
                }
            }
        }
    }
}

@Composable
@Preview(name = "AlbumCardPreview_NonGrid")
@Preview(uiMode = PREVIEW_DARK_MODE, name = "AlbumCardPreview_NonGrid_Dark")
fun AlbumCardPreview() {
    AlbumCard(
        AlbumDto(
            albumName = "Album Name",
            albumYear = 2025,
            artistName = "Artist Name",
            songCount = 12,
            albumDuration = 50 * 60 * 1000L,
        ),
    )
}
