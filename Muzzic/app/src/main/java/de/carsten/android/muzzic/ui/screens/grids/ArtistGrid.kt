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
import de.carsten.android.muzzic.ui.model.ArtistDto
import de.carsten.android.muzzic.ui.screens.cards.ArtistCard
import de.carsten.android.muzzic.viewmodel.LibraryViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun ArtistGrid(viewModel: LibraryViewModel = koinViewModel()) {
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    ArtistGridContent(artists)
}

@Composable
fun ArtistGridContent(artists: List<ArtistDto>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(artists) { artist ->
            ArtistCard(artist)
        }
    }
}

@Preview
@Composable
fun ArtistGridPreview() {
    ArtistGridContent(
        listOf(
            ArtistDto("Cradle Of Filth", 2, 13),
            ArtistDto("Dimmu Borgir", 1, 10),
            ArtistDto("Interpol", 1, 7),
            ArtistDto("Jimmy Eat World", 1, 10)
        )
    )
}
