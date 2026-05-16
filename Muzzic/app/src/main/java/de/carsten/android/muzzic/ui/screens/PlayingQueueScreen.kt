package de.carsten.android.muzzic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.component.PlayingQueueItem
import de.carsten.android.muzzic.ui.component.ReorderableLazyColumn
import de.carsten.android.muzzic.ui.theme.AppTheme
import de.carsten.android.muzzic.viewmodel.PlayingQueueViewModel
import de.carsten.android.muzzic.viewmodel.SelectionViewModel
import de.carsten.android.muzzic.viewmodel.states.SelectionState
import org.koin.androidx.compose.koinViewModel
import java.util.UUID

@Composable
fun PlayingQueueScreen(
    modifier: Modifier,
    playingQueueViewModel: PlayingQueueViewModel = koinViewModel(),
    selectionViewModel: SelectionViewModel = koinViewModel(),
) {
    val playingQueue by playingQueueViewModel.currentPlayingQueue.collectAsState()
    val selectionState by selectionViewModel.selectionState.collectAsState()
    val currentSong by playingQueueViewModel.currentSong.collectAsState()
    val isPlaying by playingQueueViewModel.isPlaying.collectAsState()
    val currentPosition by playingQueueViewModel.currentPosition.collectAsState()
    val duration by playingQueueViewModel.duration.collectAsState()

    val progress by remember {
        derivedStateOf {
            if (duration > 0) currentPosition.toFloat() / duration else 0f
        }
    }

    PlayingQueueContent(
        modifier = modifier,
        playingQueue = playingQueue,
        selectionState = selectionState,
        currentSong = currentSong,
        isPlaying = isPlaying,
        progress = progress,
        onSongLongClick = { songId -> selectionViewModel.toggleSongSelection(songId) },
        onSongClick = { index, songId ->
            if (selectionState.isActive) {
                selectionViewModel.toggleSongSelection(songId)
            } else {
                playingQueueViewModel.playSongAt(index)
            }
        },
        onMove = { from, to -> playingQueueViewModel.moveSong(from, to) },
        onTogglePlayPause = { playingQueueViewModel.togglePlayPause() }
    )
}

@Composable
fun PlayingQueueContent(
    modifier: Modifier,
    name: String = "Playing Queue",
    playingQueue: List<MediaItem>,
    selectionState: SelectionState = SelectionState(),
    currentSong: MediaItem? = null,
    isPlaying: Boolean = false,
    progress: Float = 0f,
    onSongClick: (Int, String) -> Unit = { _, _ -> },
    onSongLongClick: (String) -> Unit = {},
    onMove: (Int, Int) -> Unit = { _, _ -> },
    onTogglePlayPause: () -> Unit = {},
) {
    AppTheme {
        Column(
            modifier =
                modifier
                    .background(MaterialTheme.colorScheme.background)
                    .fillMaxSize()
                    .padding(16.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            if (playingQueue.isEmpty()) {
                Text(
                    text = stringResource(R.string.empty_pq),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            } else {
                ReorderableLazyColumn(
                    items = playingQueue,
                    onMove = onMove,
                    key = { _, item -> item.mediaId },
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 8.dp)
                ) { index, item, isDragging, dragModifier ->
                    val isSelected = selectionState.selectedSongs.contains(item.mediaId)
                    val isCurrentSong = item.mediaId == currentSong?.mediaId

                    PlayingQueueItem(
                        item = item,
                        isPlaying = isPlaying,
                        isCurrentSong = isCurrentSong,
                        progress = if (isCurrentSong) progress else 0f,
                        isDragging = isDragging,
                        isSelected = isSelected,
                        dragModifier = dragModifier,
                        onClick = { onSongClick(index, item.mediaId) },
                        onLongClick = { onSongLongClick(item.mediaId) },
                        onTogglePlayPause = {
                            if (isCurrentSong) {
                                onTogglePlayPause()
                            } else {
                                onSongClick(index, item.mediaId)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
@Preview
@Preview(uiMode = PREVIEW_DARK_MODE)
fun PlayingQueueScreenPreview() {
    PlayingQueueContent(
        modifier = Modifier.padding(2.dp),
        name = "Test Queue",
        playingQueue =
            listOf(
                MediaItem
                    .Builder()
                    .setMediaId(UUID.randomUUID().toString())
                    .setMediaMetadata(
                        MediaMetadata
                            .Builder()
                            .setTrackNumber(2)
                            .setTitle("This is a test title")
                            .setArtist("Test-Artist")
                            .setAlbumTitle("Test-Album")
                            .build(),
                    ).build(),
            ),
    )
}
