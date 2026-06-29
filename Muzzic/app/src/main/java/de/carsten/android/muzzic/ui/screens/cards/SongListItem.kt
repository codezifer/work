package de.carsten.android.muzzic.ui.screens.cards

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import de.carsten.android.muzzic.DOT
import de.carsten.android.muzzic.persistence.entity.Song
import de.carsten.android.muzzic.ui.CORNER_RADIUS_SMALL
import de.carsten.android.muzzic.ui.FONT_SIZE_BODY
import de.carsten.android.muzzic.ui.FONT_SIZE_CAPTION
import de.carsten.android.muzzic.ui.FONT_SIZE_SMALL
import de.carsten.android.muzzic.ui.ICON_SIZE_DRAG_HANDLE
import de.carsten.android.muzzic.ui.ICON_SIZE_LARGE
import de.carsten.android.muzzic.ui.ICON_SIZE_TINY
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.SPACING_NORMAL
import de.carsten.android.muzzic.ui.model.ColorSource
import de.carsten.android.muzzic.ui.theme.AppTheme
import de.carsten.android.muzzic.ui.utils.formatDuration
import java.time.Instant

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongListItem(
    song: Song,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    isSelected: Boolean = false,
    colorSource: ColorSource = ColorSource(accentColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
) {
    Card(
        modifier =
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CORNER_RADIUS_SMALL))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        colors =
        CardDefaults.cardColors(
            containerColor = if (isSelected) colorSource.accentColor else colorSource.contentColor,
        ),
        shape = RoundedCornerShape(CORNER_RADIUS_SMALL),
    ) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(SPACING_NORMAL),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Song Icon or Checkmark
            Box(
                modifier = Modifier.size(ICON_SIZE_LARGE),
                contentAlignment = Alignment.Center,
            ) {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    colors =
                    CardDefaults.cardColors(
                        containerColor = if (isSelected) colorSource.accentColor else MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                    shape = if (isSelected) CircleShape else RoundedCornerShape(CORNER_RADIUS_SMALL),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (isSelected) Icons.Default.Check else Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = if (isSelected) colorSource.contentColor else colorSource.accentColor,
                            modifier = Modifier.size(ICON_SIZE_DRAG_HANDLE),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(SPACING_NORMAL))

            // Song Info
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "${song.trackNumberFormatted()} $DOT ${song.title}",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = FONT_SIZE_BODY,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = "${song.artist} $DOT ${song.album}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = FONT_SIZE_CAPTION,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Rating and Duration
            Column(
                horizontalAlignment = Alignment.End,
            ) {
                StarRating(
                    rating = song.rating,
                    onRatingChanged = { /* Update rating */ },
                    size = ICON_SIZE_TINY,
                )

                Text(
                    text = formatDuration(song.duration),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = FONT_SIZE_SMALL,
                )
            }
        }
    }
}

@Composable
@Preview
@Preview(uiMode = PREVIEW_DARK_MODE, name = "SongListItemPreview_Dark")
fun SongListItemPreview() {
    AppTheme {
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
}
