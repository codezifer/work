package de.carsten.android.muzzic.ui.screens.grids

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.carsten.android.muzzic.ui.model.AlbumDto
import de.carsten.android.muzzic.ui.screens.cards.AlbumCard

@Composable
fun AlbumGrid(
    albums: List<AlbumDto>,
    onAlbumClick: (String, String) -> Unit = { _, _ -> },
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(albums) { album ->
            AlbumCard(
                album = album,
                grid = true,
                onClick = { onAlbumClick(album.artistName, album.albumName) }
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
