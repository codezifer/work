package de.carsten.android.muzzic.ui.screens.grids

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.model.ArtistDto
import de.carsten.android.muzzic.ui.screens.cards.ArtistCard
import de.carsten.android.muzzic.ui.screens.controls.FastScroller
import kotlinx.coroutines.launch

@Composable
fun ArtistGrid(
    modifier: Modifier = Modifier,
    artists: List<ArtistDto>,
    onArtistClick: (String) -> Unit = {},
    onArtistLongClick: (String) -> Unit = {},
    selectedArtists: Set<String> = emptySet(),
) {
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    val alphabet by remember(artists) {
        derivedStateOf {
            artists.map { it.artistName.take(1).uppercase() }.distinct().sorted()
        }
    }

    val activeLetter by remember(artists) {
        derivedStateOf {
            val index = gridState.firstVisibleItemIndex
            if (index in artists.indices) {
                artists[index].artistName.take(1).uppercase()
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
                itemsIndexed(
                    items = artists,
                    key = { _, artist -> artist.artistName },
                    contentType = { _, _ -> "Artist" }
                ) { _, artist ->
                    ArtistCard(
                        artist = artist,
                        onClick = { onArtistClick(artist.artistName) },
                        onLongClick = { onArtistLongClick(artist.artistName) },
                        isSelected = selectedArtists.contains(artist.artistName)
                    )
                }
            }

            if (alphabet.isNotEmpty()) {
                FastScroller(
                    alphabet = alphabet,
                    activeLetter = activeLetter,
                    isScrolling = gridState.isScrollInProgress,
                    onLetterSelected = { letter ->
                        val index = artists.indexOfFirst { it.artistName.startsWith(letter, ignoreCase = true) }
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

@Composable
@Preview
@Preview(uiMode = PREVIEW_DARK_MODE)
fun ArtistGridPreview() {
    ArtistGrid(
        artists = listOf(
            ArtistDto("Cradle Of Filth", 2, 13),
            ArtistDto("Dimmu Borgir", 1, 10),
            ArtistDto("Interpol", 1, 7),
            ArtistDto("Jimmy Eat World", 1, 10),
        ),
    )
}
