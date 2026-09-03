package de.carsten.android.muzzic.ui.screens.cards

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import de.carsten.android.muzzic.model.toAlbumArtUri
import de.carsten.android.muzzic.ui.CARD_CONTENT_HEIGHT
import de.carsten.android.muzzic.ui.CARD_CONTENT_SPACING
import de.carsten.android.muzzic.ui.MAINTITLE_FONTSIZE
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.model.CoverSource
import de.carsten.android.muzzic.ui.model.GenreDto
import de.carsten.android.muzzic.ui.theme.AppTheme
import de.carsten.android.muzzic.ui.utils.extractColors

@Composable
fun GenreCard(genre: GenreDto, onClick: () -> Unit = {}, onPlayClick: () -> Unit = {}, isSelected: Boolean = false, borderColor: Color = MaterialTheme.colorScheme.primary) {
    val palette by rememberPaletteState(genre.lastAlbumArt?.toAlbumArtUri())
    val colors = palette.extractColors(
        defaultBackground = MaterialTheme.colorScheme.primaryContainer,
        defaultContent = MaterialTheme.colorScheme.onPrimaryContainer,
    )
    val backgroundColor = colors.backgroundColor
    val contentColor = colors.contentColor

    val collageCovers = remember(genre.albumArts) {
        genre.albumArts.map { CoverSource.FromPath(it) }
    }

    AppTheme {
        MuzzicCard(
            header = {
                AlbumCoverCollage(
                    covers = collageCovers,
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.4f),
                    useCard = false,
                )
            },
            backgroundColor = backgroundColor,
            contentColor = contentColor,
            onClick = onClick,
            onPlayClick = onPlayClick,
            isSelected = isSelected,
            borderColor = borderColor,
            contentHeight = CARD_CONTENT_HEIGHT,
        ) {
            Text(
                text = genre.genreName,
                color = it,
                fontSize = MAINTITLE_FONTSIZE,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(CARD_CONTENT_SPACING))

            SubtitleInformation(
                listOf(
                    Pair(Icons.Default.Person, "${genre.artistCount} Artists"),
                    Pair(Icons.Default.Album, "${genre.albumCount} Albums"),
                    Pair(Icons.Default.MusicNote, "${genre.songCount} Songs"),
                ),
                fontColor = it.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
@Preview
@Preview(uiMode = PREVIEW_DARK_MODE, name = "GenreCardPreview_Dark")
fun GenreCardPreview() {
    GenreCard(
        GenreDto(
            genreName = "Black Metal",
            artistCount = 60,
            albumCount = 450,
            songCount = 4200,
            genreDuration = 24 * 60 * 60 * 1000L,
            lastAlbumArt = null,
        ),
    )
}
