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
import de.carsten.android.muzzic.ui.model.ColorSource
import de.carsten.android.muzzic.ui.model.GenreDto
import de.carsten.android.muzzic.ui.model.composableColorSource
import de.carsten.android.muzzic.ui.screens.cards.GenreCard
import de.carsten.android.muzzic.ui.theme.AppTheme

@Composable
fun GenreGrid(
    genres: List<GenreDto>,
    modifier: Modifier = Modifier,
    onGenreClick: (GenreDto) -> Unit = {},
    onGenrePlayClick: (GenreDto) -> Unit = {},
    colorSource: ColorSource = composableColorSource(),
) {
    val gridState = rememberLazyGridState()

    FastScrollBox(
        items = genres,
        getLabel = { it.genreName.take(AppConfig.Ui.NUM_OF_LETTERS) },
        firstVisibleItemIndex = { gridState.firstVisibleItemIndex },
        isScrollInProgress = { gridState.isScrollInProgress },
        scrollToItem = { index -> gridState.scrollToItem(index) },
        modifier = modifier,
        colorSource = colorSource,
    ) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(AppConfig.Ui.NUM_OF_GRID_COLUMNS),
            modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = GRID_HORIZONTAL_PADDING),
            verticalArrangement = Arrangement.spacedBy(GRID_SPACING),
            horizontalArrangement = Arrangement.spacedBy(GRID_SPACING),
        ) {
            items(
                items = genres,
                key = { it.lazyKey() },
                contentType = { it.lazyContentType() },
            ) { genre ->
                GenreCard(
                    genre = genre,
                    onClick = { onGenreClick(genre) },
                    onPlayClick = { onGenrePlayClick(genre) },
                    borderColor = colorSource.accentColor,
                )
            }
        }
    }
}

@Composable
@Preview(showBackground = true, name = "Light Mode")
@Preview(uiMode = PREVIEW_DARK_MODE, showBackground = true, name = "Dark Mode")
fun GenreGridPreview() {
    AppTheme {
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
}
