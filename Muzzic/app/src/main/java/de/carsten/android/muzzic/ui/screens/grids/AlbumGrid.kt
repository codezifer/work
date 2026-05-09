package de.carsten.android.muzzic.ui.screens.grids

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.carsten.android.muzzic.ui.model.AlbumDto
import de.carsten.android.muzzic.ui.screens.cards.AlbumCard
import de.carsten.android.muzzic.ui.screens.controls.FastScroller
import kotlinx.coroutines.launch

@Composable
fun AlbumGrid(
    albums: List<AlbumDto>,
    onAlbumClick: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    val alphabet by remember(albums) {
        derivedStateOf {
            albums.map { it.albumName.take(1).uppercase() }.distinct().sorted()
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
        Row(modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(
                    items = albums,
                    key = { album -> "${album.artistName}_${album.albumName}_${album.albumYear}" },
                    contentType = { "Album" }
                ) { album ->
                    val onClick = remember(album.artistName, album.albumName) {
                        { onAlbumClick(album.artistName, album.albumName) }
                    }
                    AlbumCard(
                        album = album,
                        onClick = onClick
                    )
                }
            }

            if (alphabet.isNotEmpty()) {
                FastScroller(
                    alphabet = alphabet,
                    activeLetter = activeLetter,
                    isScrolling = gridState.isScrollInProgress,
                    onLetterSelected = { letter ->
                        val index = albums.indexOfFirst { it.albumName.startsWith(letter, ignoreCase = true) }
                        if (index != -1) {
                            scope.launch {
                                gridState.scrollToItem(index)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Preview
@Composable
fun AlbumGridPreview() {
    AlbumGrid(
        listOf(
            AlbumDto(
                artistName = "Dimmu Borgir",
                albumName = "Enthrone Darkness Triumphant",
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
