package de.carsten.android.muzzic.ui.screens.grids

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import de.carsten.android.muzzic.ui.GRID_HORIZONTAL_PADDING
import de.carsten.android.muzzic.ui.GRID_SPACING
import de.carsten.android.muzzic.ui.SPACING_NORMAL
import de.carsten.android.muzzic.ui.model.AlbumDto
import de.carsten.android.muzzic.ui.model.ColorSource
import de.carsten.android.muzzic.ui.model.composableColorSource
import de.carsten.android.muzzic.ui.screens.cards.AlbumCard
import de.carsten.android.muzzic.ui.screens.controls.FastScroller
import kotlinx.coroutines.launch

@Composable
fun AlbumGrid(
    albums: List<AlbumDto>,
    onAlbumClick: (String, String) -> Unit = { _, _ -> },
    onAlbumPlayClick: (String, String) -> Unit = { _, _ -> },
    onAlbumLongClick: (String, String) -> Unit = { _, _ -> },
    selectedAlbums: Set<String> = emptySet(),
    modifier: Modifier = Modifier,
    colorSource: ColorSource = composableColorSource(),
) {
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    val alphabet by remember(albums) {
        derivedStateOf {
            albums.map { it.albumName.take(1).uppercase() }.distinct().sorted()
        }
    }

    val letterToIndexMap by remember(albums) {
        derivedStateOf {
            albums.foldIndexed(mutableMapOf<String, Int>()) { index, map, album ->
                val letter = album.albumName.take(1).uppercase()
                if (!map.containsKey(letter)) {
                    map[letter] = index
                }
                map
            }
        }
    }

    val activeLetter by remember(albums) {
        derivedStateOf {
            val index = gridState.firstVisibleItemIndex
            if (index in albums.indices) {
                albums[index].albumName.take(1).uppercase()
            } else {
                null
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(2),
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = GRID_HORIZONTAL_PADDING),
            verticalArrangement = Arrangement.spacedBy(GRID_SPACING),
            horizontalArrangement = Arrangement.spacedBy(GRID_SPACING),
        ) {
            items(
                items = albums,
                key = { album -> "${album.artistName}_${album.albumName}_${album.albumYear}" },
                contentType = { "Album" },
            ) { album ->
                AlbumCard(
                    album = album,
                    onClick = { onAlbumClick(album.artistName, album.albumName) },
                    onPlayClick = { onAlbumPlayClick(album.artistName, album.albumName) },
                    onLongClick = { onAlbumLongClick(album.artistName, album.albumName) },
                    isSelected = selectedAlbums.contains("${album.artistName}|${album.albumName}"),
                    borderColor = colorSource.accentColor,
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
                isScrolling = gridState.isScrollInProgress,
                onLetterSelected = { letter ->
                    letterToIndexMap[letter]?.let { index ->
                        scope.launch {
                            gridState.scrollToItem(index)
                        }
                    }
                },
                colorSource = colorSource,
            )
        }
    }
}

@Preview
@Composable
fun AlbumGridPreview() {
    AlbumGrid(
        listOf(
            AlbumDto(
                artistName = "Orchestra of the Infinite Void",
                albumName = "Echoes from the Ancient Labyrinth",
                albumYear = 1997,
                songCount = 21,
                albumDuration = 90 * 60 * 1000L,
            ),
            AlbumDto(
                artistName = "Jimmy Eat World",
                albumName = "Bleed American",
                albumYear = 2001,
                songCount = 12,
                albumDuration = 45 * 60 * 1000L,
            ),
        ),
    )
}
