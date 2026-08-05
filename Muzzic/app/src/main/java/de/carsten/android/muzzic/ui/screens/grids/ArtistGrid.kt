package de.carsten.android.muzzic.ui.screens.grids

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import de.carsten.android.muzzic.ui.GRID_HORIZONTAL_PADDING
import de.carsten.android.muzzic.ui.GRID_SPACING
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.component.FastScrollBox
import de.carsten.android.muzzic.ui.model.ArtistDto
import de.carsten.android.muzzic.ui.model.ColorSource
import de.carsten.android.muzzic.ui.model.composableColorSource
import de.carsten.android.muzzic.ui.screens.cards.ArtistCard

@Composable
fun ArtistGrid(
    modifier: Modifier = Modifier,
    artists: List<ArtistDto>,
    onArtistClick: (String) -> Unit = {},
    onArtistPlayClick: (String) -> Unit = {},
    onArtistLongClick: (String) -> Unit = {},
    selectedArtists: Set<String> = emptySet(),
    colorSource: ColorSource = composableColorSource(),
) {
    val gridState = rememberLazyGridState()

    FastScrollBox(
        items = artists,
        label = { it.artistName },
        firstVisibleItemIndex = { gridState.firstVisibleItemIndex },
        isScrollInProgress = { gridState.isScrollInProgress },
        scrollToItem = { index -> gridState.scrollToItem(index) },
        modifier = modifier,
        colorSource = colorSource,
    ) {
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
            itemsIndexed(
                items = artists,
                key = { _, artist -> artist.artistName },
                contentType = { _, _ -> "Artist" },
            ) { _, artist ->
                ArtistCard(
                    artist = artist,
                    onClick = { onArtistClick(artist.artistName) },
                    onPlayClick = { onArtistPlayClick(artist.artistName) },
                    onLongClick = { onArtistLongClick(artist.artistName) },
                    isSelected = selectedArtists.contains(artist.artistName),
                    borderColor = colorSource.accentColor,
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
        artists =
        listOf(
            ArtistDto("Cradle Of Filth", 2, 13),
            ArtistDto("Dimmu Borgir", 1, 10),
            ArtistDto("Interpol", 1, 7),
            ArtistDto("Jimmy Eat World", 1, 10),
        ),
    )
}
