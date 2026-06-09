package de.carsten.android.muzzic.ui.screens.cards

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.ui.CARD_CONTENT_HEIGHT
import de.carsten.android.muzzic.ui.CARD_CONTENT_SPACING
import de.carsten.android.muzzic.ui.MAINTITLE_FONTSIZE
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.model.AlbumDto
import de.carsten.android.muzzic.ui.theme.AppTheme
import de.carsten.android.muzzic.ui.utils.extractColors

@Composable
fun AlbumCard(album: AlbumDto, onClick: () -> Unit = {}, onLongClick: () -> Unit = {}, isSelected: Boolean = false) {
    val palette by rememberPaletteState(album.lastAlbumArt)
    val colors = palette.extractColors(
        defaultBackground = MaterialTheme.colorScheme.primaryContainer,
        defaultContent = MaterialTheme.colorScheme.onPrimaryContainer,
    )
    val backgroundColor = colors.backgroundColor
    val contentColor = colors.contentColor

    AppTheme {
        MuzzicCard(
            header = {
                Box(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = album.lastAlbumArt,
                        contentDescription = stringResource(R.string.album_art),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(R.drawable.disc),
                        error = painterResource(R.drawable.disc),
                    )
                }
            },
            backgroundColor = backgroundColor,
            contentColor = contentColor,
            onClick = onClick,
            onLongClick = onLongClick,
            isSelected = isSelected,
            contentHeight = CARD_CONTENT_HEIGHT,
        ) {
            Text(
                text = album.albumName,
                color = it,
                fontSize = MAINTITLE_FONTSIZE,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(CARD_CONTENT_SPACING))

            SubtitleInformation(
                listOf(
                    Pair(Icons.Default.Person, album.artistName),
                    Pair(Icons.Default.MusicNote, "${album.songCount} Songs"),
                    Pair(Icons.Default.CalendarMonth, album.albumYear.toString()),
                ),
                fontColor = it.copy(alpha = 0.8f),
            )
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
