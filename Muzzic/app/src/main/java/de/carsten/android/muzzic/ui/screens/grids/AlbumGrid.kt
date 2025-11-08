package de.carsten.android.muzzic.ui.screens.grids

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.carsten.android.muzzic.persistence.entity.ArtistAlbum
import de.carsten.android.muzzic.ui.screens.cards.AlbumCard
import de.carsten.android.muzzic.viewmodel.LibraryViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun AlbumGrid(viewModel: LibraryViewModel = koinViewModel()) {
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    AlbumGridContent(albums)
}

@Composable
fun AlbumGridContent(albums: List<ArtistAlbum>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(albums) { album ->
            AlbumCard(album)
        }
    }
}

@Preview
@Composable
fun AlbumGridPreview() {
    AlbumGridContent(listOf(
        ArtistAlbum("Dimmu Borgir", "Enthrone Darkness Triumphant"),
        ArtistAlbum("Jimmy Eat World", "Bleed American")
    ))
}
