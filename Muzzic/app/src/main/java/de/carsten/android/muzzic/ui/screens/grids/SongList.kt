package de.carsten.android.muzzic.ui.screens.grids

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.carsten.android.muzzic.persistence.entity.Song
import de.carsten.android.muzzic.ui.screens.cards.SongListItem
import de.carsten.android.muzzic.ui.screens.controls.FastScroller
import kotlinx.coroutines.launch
import java.time.Instant

@Composable
fun SongList(songs: List<Song>, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val alphabet by remember(songs) {
        derivedStateOf {
            songs.mapNotNull { it.title?.take(1)?.uppercase() }.distinct().sorted()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(songs) { song ->
                    SongListItem(song)
                }
            }

            if (alphabet.isNotEmpty()) {
                FastScroller(
                    alphabet = alphabet,
                    onLetterSelected = { letter ->
                        val index = songs.indexOfFirst { it.title?.startsWith(letter, ignoreCase = true) == true }
                        if (index != -1) {
                            scope.launch {
                                listState.scrollToItem(index)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
@Preview
fun SongListPreview() {
    SongList(
        listOf(
            Song(
                title = "This is just a Test",
                album = "Test-Album",
                artist = "Test-Artist",
                duration = 3 * 60 * 1000,
                genre = "Alternative",
                lastPlayed = Instant.now(),
                playCount = 3,
                rating = 5,
                totalTracks = 10,
                trackNumber = 3,
            ),
            Song(
                title = "This is just a Test",
                album = "Test-Album",
                artist = "Test-Artist",
                duration = 3 * 60 * 1000,
                genre = "Alternative",
                lastPlayed = Instant.now(),
                playCount = 2,
                rating = 3,
                totalTracks = 10,
                trackNumber = 4,
            ),
        ),
    )
}
