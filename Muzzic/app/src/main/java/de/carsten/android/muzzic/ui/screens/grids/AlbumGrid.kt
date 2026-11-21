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
import de.carsten.android.muzzic.AppConfig
import de.carsten.android.muzzic.ui.GRID_HORIZONTAL_PADDING
import de.carsten.android.muzzic.ui.GRID_SPACING
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.component.fastscroll.FastScrollBox
import de.carsten.android.muzzic.ui.model.AlbumDto
import de.carsten.android.muzzic.ui.model.ColorSource
import de.carsten.android.muzzic.ui.model.FastScrollMode
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
    fastScrollMode: FastScrollMode = FastScrollMode.FAST_SCROLLER,
) {
    val gridState = rememberLazyGridState()

    FastScrollBox(
        items = albums,
        getLabel = { it.albumName.take(AppConfig.Ui.NUM_OF_LETTERS) },
        firstVisibleItemIndex = { gridState.firstVisibleItemIndex },
        isScrollInProgress = { gridState.isScrollInProgress },
        scrollToItem = { index -> gridState.scrollToItem(index) },
        modifier = modifier,
        fastScrollMode = fastScrollMode,
        colorSource = colorSource,
    ) { visibleAlbums ->
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(AppConfig.Ui.NUM_OF_GRID_COLUMNS),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = GRID_HORIZONTAL_PADDING),
            verticalArrangement = Arrangement.spacedBy(GRID_SPACING),
            horizontalArrangement = Arrangement.spacedBy(GRID_SPACING),
        ) {
            items(
                items = visibleAlbums,
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
