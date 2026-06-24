package de.carsten.android.muzzic.ui.screens.cards

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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import de.carsten.android.muzzic.ui.CORNER_RADIUS_SMALL
import de.carsten.android.muzzic.ui.FONT_SIZE_BODY
import de.carsten.android.muzzic.ui.FONT_SIZE_CAPTION
import de.carsten.android.muzzic.ui.FONT_SIZE_SMALL
import de.carsten.android.muzzic.ui.ICON_SIZE_CONTROL_SMALL
import de.carsten.android.muzzic.ui.ICON_SIZE_PLAYLIST_THUMB
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.SPACING_MEDIUM
import de.carsten.android.muzzic.ui.SPACING_NORMAL
import de.carsten.android.muzzic.ui.SPACING_SMALL
import de.carsten.android.muzzic.ui.model.CoverSource
import de.carsten.android.muzzic.ui.model.PlaylistDto
import de.carsten.android.muzzic.ui.theme.ListItemLeadingShape
import de.carsten.android.muzzic.ui.utils.formatDuration

/**
 * A list item representing a playlist.
 *
 * @param playlist The [PlaylistDto] to display.
 * @param modifier The [Modifier] to be applied to the layout.
 * @param showGenre Whether to show the playlist's genre.
 * @param iconColor The color of the icons in the list item.
 * @param onPlaylistClick Callback triggered when the playlist item is clicked.
 * @param onPlayClick Callback triggered when the play button is clicked.
 * @param onDeleteClick Callback triggered when the delete option is selected.
 */
@Composable
fun PlaylistListItem(
    playlist: PlaylistDto,
    modifier: Modifier = Modifier,
    showGenre: Boolean = false,
    iconColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    onPlaylistClick: () -> Unit = {},
    onPlayClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
) {
    var showMenu by remember { mutableStateOf(false) }
    val collageCovers = remember(playlist.albumArts) {
        playlist.albumArts.map { CoverSource.FromPath(it) }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(CORNER_RADIUS_SMALL),
        onClick = onPlaylistClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Playlist Icon
            AlbumCoverCollage(
                covers = collageCovers,
                modifier = Modifier
                    .size(ICON_SIZE_PLAYLIST_THUMB)
                    .clip(ListItemLeadingShape),
                useCard = false,
            )

            Spacer(modifier = Modifier.width(SPACING_NORMAL))

            // Playlist Info
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = playlist.playlistName,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = FONT_SIZE_BODY,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text =
                    if (showGenre) {
                        "${playlist.playlistGenre} • ${playlist.songCount} Songs • ${playlist.artistCount} Artists"
                    } else {
                        "${playlist.songCount} Songs • ${playlist.artistCount} Artists"
                    },
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    fontSize = FONT_SIZE_CAPTION,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Duration
            Text(
                text = formatDuration(playlist.playlistDuration),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                fontSize = FONT_SIZE_SMALL,
            )

            Spacer(modifier = Modifier.width(SPACING_MEDIUM))

            // Play Button
            IconButton(
                onClick = onPlayClick,
                modifier = Modifier.size(ICON_SIZE_CONTROL_SMALL),
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play Playlist",
                    tint = iconColor,
                )
            }

            // Menu Button
            Box(modifier = Modifier.padding(end = SPACING_SMALL)) {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(ICON_SIZE_CONTROL_SMALL),
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More Options",
                        tint = iconColor,
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Löschen") },
                        onClick = {
                            showMenu = false
                            onDeleteClick()
                        },
                    )
                }
            }
        }
    }
}

@Composable
@Preview
@Preview(uiMode = PREVIEW_DARK_MODE, name = "PlaylistItemPreview_Dark")
fun PlaylistItemPreview() {
    PlaylistListItem(
        playlist =
        PlaylistDto(
            playlistId = "1",
            playlistName = "TopBlackMetal",
            playlistIsAutoGenerated = false,
            playlistGenre = "Black Metal",
            artistCount = 42,
            albumCount = 231,
            songCount = 744,
            playlistDuration = 24 * 60 * 60 * 1000L,
        ),
        showGenre = true,
    )
}
