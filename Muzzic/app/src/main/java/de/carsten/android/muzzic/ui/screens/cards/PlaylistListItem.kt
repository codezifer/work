package de.carsten.android.muzzic.ui.screens.cards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.GenericShape
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.model.CoverSource
import de.carsten.android.muzzic.ui.model.PlaylistDto
import de.carsten.android.muzzic.ui.utils.formatDuration

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

    Card(
        modifier =
        modifier
            .fillMaxWidth()
            .clickable { /* Open playlist */ },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(8.dp),
        onClick = { onPlaylistClick() },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Playlist Icon
            AlbumCoverCollage(
                covers =
                if (playlist.lastAlbumArt == null) {
                    emptyList()
                } else {
                    listOf(CoverSource.FromPath(playlist.lastAlbumArt))
                },
                modifier = Modifier
                    .size(56.dp)
                    .clip(
                        GenericShape { size, _ ->
                            val radius = 8.dp.value * size.height / 56.dp.value
                            moveTo(radius, 0f)
                            lineTo(size.width, 0f)
                            lineTo(size.width, size.height)
                            lineTo(radius, size.height)
                            arcTo(
                                rect = Rect(0f, size.height - 2 * radius, 2 * radius, size.height),
                                startAngleDegrees = 90f,
                                sweepAngleDegrees = 90f,
                                forceMoveTo = false,
                            )
                            lineTo(0f, radius)
                            arcTo(
                                rect = Rect(0f, 0f, 2 * radius, 2 * radius),
                                startAngleDegrees = 180f,
                                sweepAngleDegrees = 90f,
                                forceMoveTo = false,
                            )
                            close()
                        },
                    ),
                useCard = false,
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Playlist Info
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = playlist.playlistName,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 14.sp,
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
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Duration
            Text(
                text = formatDuration(playlist.playlistDuration),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                fontSize = 10.sp,
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Play Button
            IconButton(
                onClick = onPlayClick,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play Playlist",
                    tint = iconColor,
                )
            }

            // Menu Button
            Box(modifier = Modifier.padding(end = 4.dp)) {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp),
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
