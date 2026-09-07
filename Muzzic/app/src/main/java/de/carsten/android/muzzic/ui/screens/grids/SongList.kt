package de.carsten.android.muzzic.ui.screens.grids

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.component.FastScrollBox
import de.carsten.android.muzzic.ui.model.ColorSource
import de.carsten.android.muzzic.ui.model.SongDto
import de.carsten.android.muzzic.ui.model.composableColorSource
import de.carsten.android.muzzic.ui.screens.cards.SongListItem
import de.carsten.android.muzzic.ui.theme.AppTheme
import java.time.Instant

@Composable
fun SongList(
    songs: List<SongDto>,
    onSongClick: (SongDto) -> Unit = {},
    onSongLongClick: (SongDto) -> Unit = {},
    selectedSongs: Set<String> = emptySet(),
    modifier: Modifier = Modifier,
    colorSource: ColorSource = composableColorSource(),
) {
    val listState = rememberLazyListState()

    FastScrollBox(
        items = songs,
        label = { it.title },
        firstVisibleItemIndex = { listState.firstVisibleItemIndex },
        isScrollInProgress = { listState.isScrollInProgress },
        scrollToItem = { index -> listState.scrollToItem(index) },
        modifier = modifier,
        colorSource = colorSource,
    ) {
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
    }
}

@Composable
@Preview
@Preview(uiMode = PREVIEW_DARK_MODE)
fun SongListPreview() {
    AppTheme {
        SongList(
            listOf(
                SongDto(
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
                SongDto(
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
