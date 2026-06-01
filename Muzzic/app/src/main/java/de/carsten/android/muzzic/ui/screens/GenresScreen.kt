package de.carsten.android.muzzic.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.model.GenreDto
import de.carsten.android.muzzic.ui.navigation.MusicAppState
import de.carsten.android.muzzic.ui.screens.grids.GenreGrid
import de.carsten.android.muzzic.ui.theme.AppTheme
import de.carsten.android.muzzic.viewmodel.GenresViewModel
import de.carsten.android.muzzic.viewmodel.LibraryViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun GenresScreen(
    modifier: Modifier = Modifier,
    appState: MusicAppState,
    libraryViewModel: LibraryViewModel = koinViewModel(),
    genreViewModel: GenresViewModel = koinViewModel(),
    onGenreClick: (GenreDto) -> Unit = {},
) {
    val genres by genreViewModel.genres.collectAsStateWithLifecycle()

    GenresScreenContent(
        modifier = modifier,
        appState = appState,
        genres = genres,
        onGenreClick = onGenreClick,
    )
}

@Composable
fun GenresScreenContent(modifier: Modifier = Modifier, appState: MusicAppState? = null, genres: List<GenreDto>, onGenreClick: (GenreDto) -> Unit = {}) {
    AppTheme {
        GenreGrid(genres, modifier, onGenreClick)
    }
}

@Composable
@Preview
@Preview(uiMode = PREVIEW_DARK_MODE)
fun GenreScreenPreview() {
    GenresScreenContent(
        genres =
        listOf(
            GenreDto("Black Metal", 10, 100, 1000, 50000),
            GenreDto("Alternative", 20, 200, 2000, 100000),
            GenreDto("Pagan Metal", 1, 5, 75, 5000),
        ),
    )
}
