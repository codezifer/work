package de.carsten.android.muzzic.ui.screens.cards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
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
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Playlist Icon
            Card(
                modifier = Modifier.size(40.dp),
                colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
                shape = RoundedCornerShape(8.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.PlaylistPlay,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

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
            Box {
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
