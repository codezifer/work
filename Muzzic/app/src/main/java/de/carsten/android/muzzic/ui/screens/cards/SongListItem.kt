package de.carsten.android.muzzic.ui.screens.cards

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import de.carsten.android.muzzic.DOT
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.persistence.entity.Song
import de.carsten.android.muzzic.ui.BLUR_RADIUS_LARGE
import de.carsten.android.muzzic.ui.CORNER_RADIUS_SMALL
import de.carsten.android.muzzic.ui.FONT_SIZE_BODY
import de.carsten.android.muzzic.ui.FONT_SIZE_CAPTION
import de.carsten.android.muzzic.ui.FONT_SIZE_SMALL
import de.carsten.android.muzzic.ui.GLASS_BORDER_ALPHA
import de.carsten.android.muzzic.ui.GLASS_BORDER_ALPHA_STRONG
import de.carsten.android.muzzic.ui.GLASS_BORDER_WIDTH
import de.carsten.android.muzzic.ui.GLASS_OVERLAY_ALPHA_HIGH
import de.carsten.android.muzzic.ui.GLASS_OVERLAY_ALPHA_NORMAL
import de.carsten.android.muzzic.ui.ICON_SIZE_DRAG_HANDLE
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
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CORNER_RADIUS_SMALL))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        colors =
        CardDefaults.cardColors(
            containerColor = Color.Transparent,
        ),
        shape = RoundedCornerShape(CORNER_RADIUS_SMALL),
        border =
        BorderStroke(
            width = if (isSelected) GLASS_BORDER_WIDTH * 2 else GLASS_BORDER_WIDTH,
            color =
            if (isSelected) {
                colorSource.accentColor.copy(alpha = GLASS_BORDER_ALPHA_STRONG)
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = GLASS_BORDER_ALPHA)
            },
        ),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // 1. Blurred Background Image
            AsyncImage(
                model =
                ImageRequest.Builder(LocalContext.current)
                    .data(song.albumArt)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier =
                Modifier
                    .matchParentSize()
                    .blur(BLUR_RADIUS_LARGE),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.disc),
                error = painterResource(R.drawable.disc),
            )

            // 2. Glass Overlay
            Box(
                modifier =
                Modifier
                    .matchParentSize()
                    .background(
                        if (isSelected) {
                            colorSource.accentColor.copy(alpha = GLASS_OVERLAY_ALPHA_HIGH)
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = GLASS_OVERLAY_ALPHA_NORMAL)
                        },
                    ),
            )

            // 3. Content
            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(SPACING_NORMAL),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = colorSource.contentColor,
                        modifier = Modifier.size(ICON_SIZE_DRAG_HANDLE),
                    )
                    Spacer(modifier = Modifier.width(SPACING_NORMAL))
                }

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
}

@Composable
@Preview
@Preview(uiMode = PREVIEW_DARK_MODE, name = "SongListItemPreview_Dark")
fun SongListItemPreview() {
    AppTheme {
        SongListItem(
            song =
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
