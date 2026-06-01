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
import de.carsten.android.muzzic.ui.model.GenreDto
import de.carsten.android.muzzic.ui.screens.cards.GenreCard

@Composable
fun GenreGrid(genres: List<GenreDto>, modifier: Modifier = Modifier, onGenreClick: (GenreDto) -> Unit = {}) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 170.dp),
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(
            items = genres,
            key = { genre -> genre.genreName },
            contentType = { "Genre" },
        ) { genre ->
            GenreCard(
                genre = genre,
                onClick = {
                    onGenreClick(genre)
                },
            )
        }
    }
}

@Composable
@Preview
fun GenreGridPreview() {
    GenreGrid(
        listOf(
            GenreDto(
                genreName = "Alternative",
                artistCount = 120,
                albumCount = 980,
                songCount = 9900,
                genreDuration = 2 * 24 * 60 * 60 * 1000L,
            ),
            GenreDto(
                genreName = "Black Metal",
                artistCount = 60,
                albumCount = 450,
                songCount = 4200,
                genreDuration = 24 * 60 * 60 * 1000L,
            ),
            GenreDto(
                genreName = "Cold Wave",
                artistCount = 20,
                albumCount = 50,
                songCount = 1000,
                genreDuration = 12 * 60 * 60 * 1000L,
            ),
            GenreDto(
                genreName = "Post Punk",
                artistCount = 20,
                albumCount = 50,
                songCount = 1000,
                genreDuration = 12 * 60 * 60 * 1000L,
            ),
        ),
    )
}
