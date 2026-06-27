package de.carsten.android.muzzic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.media3.common.MediaItem
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.ui.GLASS_CONTAINER_ALPHA
import de.carsten.android.muzzic.ui.PLAYING_QUEUE
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.SPACING_LARGE
import de.carsten.android.muzzic.ui.SPACING_MEDIUM
import de.carsten.android.muzzic.ui.SPACING_TINY
import de.carsten.android.muzzic.ui.component.ReorderableLazyColumn
import de.carsten.android.muzzic.ui.model.ColorSource
import de.carsten.android.muzzic.ui.model.PlayingQueueDto
import de.carsten.android.muzzic.ui.navigation.MusicAppState
import de.carsten.android.muzzic.ui.screens.cards.PlayingQueueItem
import de.carsten.android.muzzic.ui.theme.AppTheme
import de.carsten.android.muzzic.viewmodel.PlayingQueueViewModel
import de.carsten.android.muzzic.viewmodel.SelectionViewModel
import de.carsten.android.muzzic.viewmodel.states.SelectionState
import org.koin.androidx.compose.koinViewModel

@Composable
fun PlayingQueueScreen(
    modifier: Modifier,
    appState: MusicAppState,
    colorSource: ColorSource = ColorSource(accentColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
    playingQueueViewModel: PlayingQueueViewModel,
    selectionViewModel: SelectionViewModel = koinViewModel()
) {
    val playingQueue by playingQueueViewModel.currentPlayingQueue.collectAsState()
    val playingQueueName by playingQueueViewModel.currentName.collectAsState()
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

    AppTheme {
        PlayingQueueContent(
            modifier = modifier,
            name = playingQueueName,
            playingQueue = playingQueue,
            selectionState = selectionState,
            currentSong = currentSong,
            isPlaying = isPlaying,
            progress = progress,
            colorSource = colorSource,
            onSongLongClick = { songId -> selectionViewModel.toggleSongSelection(songId) },
            onSongClick = { index, songId ->
                if (selectionState.isActive) {
                    selectionViewModel.toggleSongSelection(songId)
                } else {
                    playingQueueViewModel.playSongAt(index)
                }
            },
            onMove = { from, to -> playingQueueViewModel.moveSong(from, to) },
            onTogglePlayPause = { playingQueueViewModel.togglePlayPause() },
            onSortByMetadata = { playingQueueViewModel.sortQueueByMetadata() },
            onShuffle = { playingQueueViewModel.shuffleQueue() },
        )
    }
}

@Composable
fun PlayingQueueContent(
    modifier: Modifier,
    name: String = PLAYING_QUEUE,
    playingQueue: List<PlayingQueueDto>,
    selectionState: SelectionState = SelectionState(),
    currentSong: MediaItem? = null,
    isPlaying: Boolean = false,
    progress: Float = 0f,
    colorSource: ColorSource = ColorSource(accentColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
    onSongClick: (Int, String) -> Unit = { _, _ -> },
    onSongLongClick: (String) -> Unit = {},
    onMove: (Int, Int) -> Unit = { _, _ -> },
    onTogglePlayPause: () -> Unit = {},
    onSortByMetadata: () -> Unit = {},
    onShuffle: () -> Unit = {},
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier =
        modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = GLASS_CONTAINER_ALPHA))
            .fillMaxSize()
            .padding(SPACING_LARGE),
    ) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = SPACING_MEDIUM),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Box {
                IconButton(onClick = { showSortMenu = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = stringResource(R.string.sort),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.sort_metadata)) },
                        onClick = {
                            onSortByMetadata()
                            showSortMenu = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.shuffle)) },
                        onClick = {
                            onShuffle()
                            showSortMenu = false
                        },
                    )
                }
            }
        }

        if (playingQueue.isEmpty()) {
            Text(
                text = stringResource(R.string.empty_pq),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        } else {
            ReorderableLazyColumn(
                items = playingQueue,
                onMove = onMove,
                key = { _, item -> item.mediaId },
                modifier =
                Modifier
                    .weight(1f)
                    .padding(top = SPACING_MEDIUM),
            ) { index, item, isDragging, dragModifier ->
                val isSelected = selectionState.selectedSongs.contains(item.mediaId)
                val isCurrentSong = item.mediaId == currentSong?.mediaId

                PlayingQueueItem(
                    playingQueueDto = item,
                    isPlaying = isPlaying,
                    isCurrentSong = isCurrentSong,
                    progress = if (isCurrentSong) progress else 0f,
                    isDragging = isDragging,
                    isSelected = isSelected,
                    dragModifier = dragModifier,
                    colorSource = colorSource,
                    onClick = { onSongClick(index, item.mediaId) },
                    onLongClick = { onSongLongClick(item.mediaId) },
                    onTogglePlayPause = {
                        if (isCurrentSong) {
                            onTogglePlayPause()
                        } else {
                            onSongClick(index, item.mediaId)
                        }
                    },
                )
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
@Preview(uiMode = PREVIEW_DARK_MODE, showBackground = true)
fun PlayingQueueScreenPreview() {
    AppTheme {
        PlayingQueueContent(
            modifier = Modifier.padding(SPACING_TINY),
            name = "Test Queue",
            playingQueue =
            listOf(
                PlayingQueueDto(
                    title = "Puritania",
                    album = "Puritanical Euphoric Misantropia",
                    artist = "Dimmu Borgir",
                    trackNumber = 5,
                    totalTracks = 12,
                    duration = 180000,
                    genre = "Black Metal",
                    queuePosition = 5,
                    enqueued = true,
                ),
            ),
        )
    }
}
