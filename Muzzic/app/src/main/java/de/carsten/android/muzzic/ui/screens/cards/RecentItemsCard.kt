package de.carsten.android.muzzic.ui.screens.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import de.carsten.android.muzzic.ui.FONT_SIZE_CAPTION
import de.carsten.android.muzzic.ui.FONT_SIZE_SUBTITLE
import de.carsten.android.muzzic.ui.GRID_SPACING
import de.carsten.android.muzzic.ui.ICON_SIZE_TINY
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.RECENT_CARD_WIDTH
import de.carsten.android.muzzic.ui.SPACING_LARGE
import de.carsten.android.muzzic.ui.SPACING_SMALL
import de.carsten.android.muzzic.ui.SPACING_TINY
import de.carsten.android.muzzic.ui.model.AlbumDto
import de.carsten.android.muzzic.ui.model.ArtistDto
import de.carsten.android.muzzic.ui.theme.AppTheme
import de.carsten.android.muzzic.ui.utils.formatDate
import java.time.Instant

/**
 * Shared scaffold for "recently added" rubrics: a titled [MuzzicCard] with a
 * horizontal row of fixed-width item cards, each carrying its addition date.
 *
 * @param T the item type.
 * @param title the rubric title.
 * @param items the items, ordered by addition recency descending.
 * @param key stable key per item.
 * @param contentType content type per item.
 * @param addedAt addition date per item, null hides the date row.
 * @param itemContent the card rendered per item.
 */
@Composable
fun <T> RecentItemsCard(title: String, items: List<T>, key: (T) -> Any, contentType: (T) -> Any?, addedAt: (T) -> Instant?, itemContent: @Composable (T) -> Unit) {
    val rowState = rememberLazyListState()

    MuzzicCard(
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = FONT_SIZE_SUBTITLE,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = SPACING_LARGE),
        )

        LazyRow(
            state = rowState,
            horizontalArrangement = Arrangement.spacedBy(GRID_SPACING),
        ) {
            items(
                count = items.size,
                key = { index -> key(items[index]) },
                contentType = { index -> contentType(items[index]) },
            ) { index ->
                val item = items[index]
                Column(modifier = Modifier.width(RECENT_CARD_WIDTH)) {
                    itemContent(item)
                    addedAt(item)?.let { added ->
                        val date = remember(added) { formatDate(added) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = SPACING_SMALL),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(ICON_SIZE_TINY),
                            )
                            Spacer(modifier = Modifier.width(SPACING_TINY))
                            Text(
                                text = date,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = FONT_SIZE_CAPTION,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true, name = "Light Mode")
@Preview(uiMode = PREVIEW_DARK_MODE, showBackground = true, name = "Dark Mode")
fun RecentItemsArtistsPreview() {
    AppTheme {
        RecentItemsCard(
            title = "Recently added artists",
            items = listOf(
                ArtistDto("Cradle Of Filth", 2, 13, addedAt = Instant.now()),
                ArtistDto("Dimmu Borgir", 1, 10, addedAt = Instant.now()),
                ArtistDto("Interpol", 1, 7),
            ),
            key = { it.lazyKey() },
            contentType = { it.lazyContentType() },
            addedAt = { it.addedAt },
        ) { artist ->
            ArtistCard(artist = artist)
        }
    }
}

@Composable
@Preview(showBackground = true, name = "Light Mode")
@Preview(uiMode = PREVIEW_DARK_MODE, showBackground = true, name = "Dark Mode")
fun RecentItemsAlbumsPreview() {
    AppTheme {
        RecentItemsCard(
            title = "Recently added albums",
            items = listOf(
                AlbumDto(
                    artistName = "Jimmy Eat World",
                    albumName = "Bleed American",
                    albumYear = 2001,
                    songCount = 12,
                    albumDuration = 45 * 60 * 1000L,
                    addedAt = Instant.now(),
                ),
                AlbumDto(
                    artistName = "Interpol",
                    albumName = "Turn On the Bright Lights",
                    albumYear = 2002,
                    songCount = 11,
                    albumDuration = 49 * 60 * 1000L,
                ),
            ),
            key = { it.lazyKey() },
            contentType = { it.lazyContentType() },
            addedAt = { it.addedAt },
        ) { album ->
            AlbumCard(album = album)
        }
    }
}
