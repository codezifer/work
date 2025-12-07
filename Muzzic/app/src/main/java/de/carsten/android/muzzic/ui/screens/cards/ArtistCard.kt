package de.carsten.android.muzzic.ui.screens.cards

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.carsten.android.muzzic.ui.MAINTITLE_FONTSIZE
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.model.ArtistDto
import de.carsten.android.muzzic.ui.model.CoverSource
import de.carsten.android.muzzic.ui.theme.AppTheme

@Composable
fun ArtistCard(artist: ArtistDto) {
    AppTheme {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(12.dp),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Album Cover Collage (3x3 grid)
                AlbumCoverCollage(
                    covers =
                        if (artist.lastAlbumArt ==
                            null
                        ) {
                            emptyList()
                        } else {
                            listOf(CoverSource.FromPath(artist.lastAlbumArt))
                        },
                    modifier = Modifier.size(120.dp),
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = artist.artistName,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = MAINTITLE_FONTSIZE,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(4.dp))

                SubtitleInformation(
                    listOf(
                        Pair(Icons.Default.Album, "${artist.albumCount} Albums"),
                        Pair(Icons.Default.MusicNote, "${artist.songCount} Songs"),
                    ),
                    fontColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
@Preview
@Preview(uiMode = PREVIEW_DARK_MODE, name = "ArtistCardPreview_Dark")
fun ArtistCardPreview() {
    ArtistCard(
        ArtistDto(
            artistName = "Dimmu Borgir",
            albumCount = 10,
            songCount = 123,
            lastAlbumArt = null,
        ),
    )
}
