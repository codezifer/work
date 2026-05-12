package de.carsten.android.muzzic.ui.screens.cards

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
fun ArtistCard(
    artist: ArtistDto,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    isSelected: Boolean = false,
) {
    val palette by rememberPaletteState(artist.lastAlbumArt)
    val backgroundColor = Color(palette?.getDominantColor(MaterialTheme.colorScheme.primaryContainer.hashCode()) ?: MaterialTheme.colorScheme.primaryContainer.hashCode())
    val contentColor = Color(palette?.dominantSwatch?.bodyTextColor ?: MaterialTheme.colorScheme.onPrimaryContainer.hashCode())

    AppTheme {
        MuzzicCard(
            header = {
                // Album Cover Collage - Edge to Edge
                AlbumCoverCollage(
                    covers =
                        if (artist.lastAlbumArt == null) {
                            emptyList()
                        } else {
                            listOf(CoverSource.FromPath(artist.lastAlbumArt))
                        },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.2f),
                    useCard = false,
                )
            },
            backgroundColor = backgroundColor,
            contentColor = contentColor,
            onClick = onClick,
            onLongClick = onLongClick,
            isSelected = isSelected,
        ) {
            Text(
                text = artist.artistName,
                color = it,
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
                fontColor = it.copy(alpha = 0.8f),
            )
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
