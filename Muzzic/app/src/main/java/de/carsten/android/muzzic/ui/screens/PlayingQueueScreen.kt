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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.viewmodel.PlayingQueueViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun PlayingQueueScreen(modifier: Modifier) {
    val viewModel: PlayingQueueViewModel = koinViewModel()
    val playingQueue by viewModel.currentPlayingQueue.collectAsState()

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
                style = MaterialTheme.typography.headlineSmall
            )
            IconButton(onClick = { viewModel.clear() }) {
                Icon(
                    imageVector = Icons.Filled.ClearAll,
                    contentDescription = stringResource(R.string.clear_all)
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
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${item.mediaMetadata.artist} - ${item.mediaMetadata.albumTitle}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun PlayingQueueScreenPreview() {
    PlayingQueueScreen(Modifier.padding(2.dp))
}
