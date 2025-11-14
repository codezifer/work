package de.carsten.android.muzzic.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.viewmodel.PlayingQueueViewModel
import org.koin.androidx.compose.koinViewModel
import java.util.UUID

@Composable
fun PlayingQueueScreen(modifier: Modifier, viewModel: PlayingQueueViewModel = koinViewModel()) {
    val playingQueue by viewModel.currentPlayingQueue.collectAsState()

    PlayingQueueContent(
        modifier = modifier,
        playingQueue = playingQueue,
        onClear = viewModel::clear
    )
}

@Composable
fun PlayingQueueContent(
    modifier: Modifier,
    playingQueue: List<MediaItem>,
    onClear: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Playing Queue",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onClear) {
                Icon(
                    imageVector = Icons.Filled.ClearAll,
                    contentDescription = stringResource(R.string.clear_all),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (playingQueue.isEmpty()) {
            Text(
                text = stringResource(R.string.empty_pq),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(playingQueue, key = { item -> item.mediaId }) { item ->
                    Row(
                        modifier = modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${item.mediaMetadata.trackNumber} - ${item.mediaMetadata.title}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${item.mediaMetadata.artist} - ${item.mediaMetadata.albumTitle}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
@Preview
@Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
fun PlayingQueueScreenPreview() {
    PlayingQueueContent(
        modifier = Modifier.padding(2.dp),
        playingQueue = listOf(
            MediaItem.Builder()
                .setMediaId(UUID.randomUUID().toString())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTrackNumber(2)
                        .setTitle("This is a test title")
                        .setAlbumArtist("Test-Artist")
                        .setAlbumTitle("Test-Album")
                        .build()
                )
                .build()
        ),
        onClear = {}
    )
}
