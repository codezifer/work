package de.carsten.android.muzzic.ui.screens.grids

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import de.carsten.android.muzzic.AppConfig
import de.carsten.android.muzzic.UNKNOWN
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.SPACING_LARGE
import de.carsten.android.muzzic.ui.SPACING_MEDIUM
import de.carsten.android.muzzic.ui.component.fastscroll.FastScrollBox
import de.carsten.android.muzzic.ui.model.ColorSource
import de.carsten.android.muzzic.ui.model.FastScrollMode
import de.carsten.android.muzzic.ui.model.SongDto
import de.carsten.android.muzzic.ui.model.composableColorSource
import de.carsten.android.muzzic.ui.screens.cards.SongListItem
import de.carsten.android.muzzic.ui.theme.AppTheme
import de.carsten.android.muzzic.ui.utils.artistName
import java.time.Instant
import kotlin.uuid.Uuid

@Composable
fun SongList(
    songs: List<SongDto>,
    onSongClick: (SongDto) -> Unit = {},
    onSongLongClick: (SongDto) -> Unit = {},
    selectedSongs: Set<String> = emptySet(),
    modifier: Modifier = Modifier,
    colorSource: ColorSource = composableColorSource(),
    fastScrollMode: FastScrollMode = FastScrollMode.FAST_SCROLLER,
) {
    val listState = rememberLazyListState()

    FastScrollBox(
        items = songs,
        getLabel = {
            if (fastScrollMode == FastScrollMode.FAST_SCROLLER) {
                it.title.take(AppConfig.Ui.NUM_OF_LETTERS)
            } else {
                artistName(it.artist).take(AppConfig.Ui.NUM_OF_LETTERS)
            }
        },
        firstVisibleItemIndex = { listState.firstVisibleItemIndex },
        isScrollInProgress = { listState.isScrollInProgress },
        scrollToItem = { index -> listState.scrollToItem(index) },
        modifier = modifier,
        fastScrollMode = fastScrollMode,
        colorSource = colorSource,
    ) { visibleSongs ->
        LazyColumn(
            state = listState,
            modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = SPACING_LARGE),
            verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM),
        ) {
            itemsIndexed(
                items = visibleSongs,
                key = { index, song ->
                    if (song.id != UNKNOWN && song.id.isNotBlank()) {
                        song.id
                    } else {
                        "${song.title}_$index"
                    }
                },
                contentType = { _, song -> song.lazyContentType() },
            ) { _, song ->
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
@Preview(showBackground = true, name = "Light Mode")
@Preview(uiMode = PREVIEW_DARK_MODE, showBackground = true, name = "Dark Mode")
fun SongListPreview() {
    AppTheme {
        SongList(
            listOf(
                SongDto(
                    id = Uuid.random().toString(),
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
                    id = Uuid.random().toString(),
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
                SongDto(
                    id = Uuid.random().toString(),
                    title = "This is just a Test 3",
                    album = "Test-Album-With-Looooooooooooooooooooooooooooooooooooooooooong Title",
                    artist = "Test-Artist",
                    duration = 3 * 60 * 1000,
                    genre = "Alternative",
                    lastPlayed = Instant.now(),
                    playCount = 2,
                    rating = 3,
                    totalTracks = 10,
                    trackNumber = 5,
                ),
            ),
        )
    }
}
