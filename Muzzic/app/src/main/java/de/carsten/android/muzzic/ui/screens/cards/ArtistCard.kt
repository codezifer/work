package de.carsten.android.muzzic.ui.screens.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.carsten.android.muzzic.ui.containerColor
import de.carsten.android.muzzic.ui.model.ArtistDto

@Composable
fun ArtistCard(artist: ArtistDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Album Cover Collage (3x3 grid)
            AlbumCoverCollage(
                covers = if (artist.lastAlbumArt == null) emptyList() else listOf(artist.lastAlbumArt),
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = artist.artistName,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "${artist.albumCount} Alben",
                    color = Color.Gray,
                    fontSize = 10.sp,
                )

                Text(
                    text = "${artist.songCount} Songs",
                    color = Color.Gray,
                    fontSize = 10.sp,
                )
            }
        }
    }
}

@Composable
@Preview
fun ArtistCardPreview() {
    ArtistCard(
        ArtistDto(
            artistName = "Dimmu Borgir",
            albumCount = 10,
            songCount = 123,
            lastAlbumArt = null,
        )
    )
}
