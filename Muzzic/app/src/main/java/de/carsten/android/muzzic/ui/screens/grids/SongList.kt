package de.carsten.android.muzzic.ui.screens.grids

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.carsten.android.muzzic.persistence.entity.Song
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.SPACING_NORMAL
import de.carsten.android.muzzic.ui.model.ColorSource
import de.carsten.android.muzzic.ui.model.composableColorSource
import de.carsten.android.muzzic.ui.screens.cards.SongListItem
import de.carsten.android.muzzic.ui.screens.controls.FastScroller
import de.carsten.android.muzzic.ui.theme.AppTheme
import kotlinx.coroutines.launch
import java.time.Instant

@Composable
fun SongList(
    songs: List<Song>,
    onSongClick: (Song) -> Unit = {},
    onSongLongClick: (Song) -> Unit = {},
    selectedSongs: Set<String> = emptySet(),
    modifier: Modifier = Modifier,
    colorSource: ColorSource = composableColorSource(),
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val alphabet by remember(songs) {
        derivedStateOf {
            songs.map { it.title.take(1).uppercase() }.distinct().sorted()
        }
    }

    val letterToIndexMap by remember(songs) {
        derivedStateOf {
            songs.foldIndexed(mutableMapOf<String, Int>()) { index, map, song ->
                val letter = song.title.take(1).uppercase()
                if (!map.containsKey(letter)) {
                    map[letter] = index
                }
                map
            }
        }
    }

    val activeLetter by remember(songs) {
        derivedStateOf {
            val index = listState.firstVisibleItemIndex
            if (index in songs.indices) {
                songs[index].title.take(1).uppercase()
            } else {
                null
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(
                items = songs,
                key = { song -> song.id },
                contentType = { "Song" },
            ) { song ->
                SongListItem(
                    song = song,
                    onClick = { onSongClick(song) },
                    onLongClick = { onSongLongClick(song) },
                    isSelected = selectedSongs.contains(song.id),
                    colorSource = colorSource,
                )
            }
        }

        if (alphabet.isNotEmpty()) {
            FastScroller(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = SPACING_NORMAL),
                alphabet = alphabet,
                activeLetter = activeLetter,
                isScrolling = listState.isScrollInProgress,
                onLetterSelected = { letter ->
                    letterToIndexMap[letter]?.let { index ->
                        scope.launch {
                            listState.scrollToItem(index)
                        }
                    }
                },
                colorSource = colorSource,
            )
        }
    }
}

@Composable
@Preview
@Preview(uiMode = PREVIEW_DARK_MODE)
fun SongListPreview() {
    AppTheme {
        SongList(
            listOf(
                Song(
                    title = "This is just a Test 1",
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
                    title = "This is just a Test 2",
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
}
