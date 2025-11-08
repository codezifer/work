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
import de.carsten.android.muzzic.ui.screens.cards.GenreCard
import de.carsten.android.muzzic.viewmodel.LibraryViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun GenreGrid(viewModel: LibraryViewModel = koinViewModel()) {
    val genres by viewModel.genres.collectAsStateWithLifecycle()
    GenreGridContent(genres)
}

@Composable
fun GenreGridContent(genres: List<String>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(genres) { genre ->
            GenreCard(genre)
        }
    }
}

@Composable
@Preview
fun GenreGridPreview() {
    GenreGridContent(
        listOf(
            "Alternative",
            "Black Metal",
            "Cold Wave",
            "Dark Wave",
            "Post Punk"
        )
    )
}
