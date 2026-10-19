package de.carsten.android.muzzic.ui.screens.grids

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import de.carsten.android.muzzic.ui.COLLAGE_COVER_MAX_COUNT
import de.carsten.android.muzzic.ui.COLLAGE_TILE_PX
import de.carsten.android.muzzic.ui.COVER_KEY_PALETTE
import de.carsten.android.muzzic.ui.GRID_HORIZONTAL_PADDING
import de.carsten.android.muzzic.ui.GRID_SPACING
import de.carsten.android.muzzic.ui.PALETTE_SIZE_PX
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.component.CoverPreloadSpec
import de.carsten.android.muzzic.ui.component.FastScrollBox
import de.carsten.android.muzzic.ui.component.LazyGridPrefetchEffect
import de.carsten.android.muzzic.ui.model.ArtistDto
import de.carsten.android.muzzic.ui.model.ColorSource
import de.carsten.android.muzzic.ui.model.composableColorSource
import de.carsten.android.muzzic.ui.screens.cards.ArtistCard
import de.carsten.android.muzzic.ui.theme.AppTheme
import de.carsten.android.muzzic.ui.utils.artistName

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
    val gridState: LazyGridState = rememberLazyGridState()

    LazyGridPrefetchEffect(
        gridState = gridState,
        items = artists,
    ) { artist ->
        buildList {
            artist.albumArts.take(COLLAGE_COVER_MAX_COUNT).forEach { path ->
                add(CoverPreloadSpec(data = path, cacheKey = path, sizePx = COLLAGE_TILE_PX))
            }
            artist.lastAlbumArt?.let { art ->
                add(
                    CoverPreloadSpec(
                        data = art,
                        cacheKey = art,
                        sizePx = PALETTE_SIZE_PX,
                        allowHardware = false,
                        keySuffix = COVER_KEY_PALETTE,
                    ),
                )
            }
        }
    }

    FastScrollBox(
        items = artists,
        label = { artistName(it.artistName) },
        firstVisibleItemIndex = { gridState.firstVisibleItemIndex },
        isScrollInProgress = { gridState.isScrollInProgress },
        scrollToItem = { index -> gridState.scrollToItem(index) },
        modifier = modifier,
        colorSource = colorSource,
    ) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = GRID_HORIZONTAL_PADDING),
            verticalArrangement = Arrangement.spacedBy(GRID_SPACING),
            horizontalArrangement = Arrangement.spacedBy(GRID_SPACING),
        ) {
            items(
                items = artists,
                key = { it.lazyKey() },
                contentType = { it.lazyContentType() },
            ) { artist ->
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
@Preview(showBackground = true, name = "Light Mode")
@Preview(uiMode = PREVIEW_DARK_MODE, showBackground = true, name = "Dark Mode")
fun ArtistGridPreview() {
    AppTheme {
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
}
