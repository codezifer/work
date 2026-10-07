package de.carsten.android.muzzic.ui.screens.grids

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import de.carsten.android.muzzic.model.toAlbumArtUri
import de.carsten.android.muzzic.ui.CARD_COVER_PX
import de.carsten.android.muzzic.ui.GRID_HORIZONTAL_PADDING
import de.carsten.android.muzzic.ui.GRID_SPACING
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.component.CoverPreloadSpec
import de.carsten.android.muzzic.ui.component.FastScrollBox
import de.carsten.android.muzzic.ui.component.LazyGridPrefetchEffect
import de.carsten.android.muzzic.ui.model.AlbumDto
import de.carsten.android.muzzic.ui.model.ColorSource
import de.carsten.android.muzzic.ui.model.composableColorSource
import de.carsten.android.muzzic.ui.screens.cards.AlbumCard
import de.carsten.android.muzzic.ui.theme.AppTheme

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

    LazyGridPrefetchEffect(
        gridState = gridState,
        items = albums,
    ) { album ->
        buildList {
            album.lastAlbumArt?.let { art ->
                add(CoverPreloadSpec(data = art, cacheKey = art, sizePx = CARD_COVER_PX))
                add(CoverPreloadSpec(data = art.toAlbumArtUri(), cacheKey = null, allowHardware = false))
            }
        }
    }

    FastScrollBox(
        items = albums,
        label = { it.albumName },
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
                items = albums,
                key = { it.lazyKey() },
                contentType = { it.lazyContentType() },
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
    }
}

@Composable
@Preview(showBackground = true, name = "Light Mode")
@Preview(uiMode = PREVIEW_DARK_MODE, showBackground = true, name = "Dark Mode")
fun AlbumGridPreview() {
    AppTheme {
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
}
