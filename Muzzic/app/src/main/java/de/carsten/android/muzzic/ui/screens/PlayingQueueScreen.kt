package de.carsten.android.muzzic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.theme.AppTheme
import de.carsten.android.muzzic.viewmodel.PlayingQueueViewModel
import org.koin.androidx.compose.koinViewModel
import java.util.UUID

@Composable
fun PlayingQueueScreen(
    modifier: Modifier,
    viewModel: PlayingQueueViewModel = koinViewModel(),
) {
    val playingQueue: List<MediaItem> by viewModel.currentPlayingQueue.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadPlayingQueue()
    }

    PlayingQueueContent(
        modifier = modifier,
        playingQueue = playingQueue,
    )
}

@Composable
fun PlayingQueueContent(
    modifier: Modifier,
    name: String = "Playing Queue",
    playingQueue: List<MediaItem>,
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
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(playingQueue, key = { item -> item.mediaId }) { item ->
                        Row(
                            modifier =
                                modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${
                                        "%02d".format(
                                            item.mediaMetadata.trackNumber,
                                        )
                                    } - ${item.mediaMetadata.title}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "${item.mediaMetadata.artist} - ${item.mediaMetadata.albumTitle}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
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
