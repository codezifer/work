package de.carsten.android.muzzic.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.model.ArtistDto
import de.carsten.android.muzzic.ui.model.ColorSource
import de.carsten.android.muzzic.ui.navigation.MusicAppState
import de.carsten.android.muzzic.ui.screens.grids.ArtistGrid
import de.carsten.android.muzzic.ui.theme.AppTheme
import de.carsten.android.muzzic.viewmodel.GenresViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun GenreArtistsScreen(
    modifier: Modifier = Modifier,
    appState: MusicAppState,
    colorSource: ColorSource = ColorSource(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.inversePrimary),
    onArtistClick: (String) -> Unit = {},
    onBackClick: () -> Unit = {},
    genresViewModel: GenresViewModel = koinViewModel(),
) {
    val artists by genresViewModel.artists.collectAsStateWithLifecycle()

    AppTheme {
        GenreArtistsScreenContent(
            modifier = modifier,
            appState = appState,
            genreName = genresViewModel.genreName ?: "",
            artists = artists,
            colorSource = colorSource,
            onArtistClick = onArtistClick,
            onArtistPlayClick = { genresViewModel.playArtist(it) },
            onBackClick = onBackClick,
        )
    }
}

@Composable
fun GenreArtistsScreenContent(
    modifier: Modifier = Modifier,
    appState: MusicAppState? = null,
    genreName: String,
    artists: List<ArtistDto>,
    colorSource: ColorSource = ColorSource(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.inversePrimary),
    onArtistClick: (String) -> Unit = {},
    onArtistPlayClick: (String) -> Unit = {},
    onBackClick: () -> Unit = {},
) {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = genreName,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        ArtistGrid(
            artists = artists,
            onArtistClick = onArtistClick,
            onArtistPlayClick = onArtistPlayClick,
            borderColor = colorSource.accentColor,
        )
    }
}

@Composable
@Preview(showBackground = true)
@Preview(uiMode = PREVIEW_DARK_MODE, showBackground = true)
fun GenreArtistsScreenContentPreview() {
    AppTheme {
        GenreArtistsScreenContent(
            genreName = "Black Metal",
            artists = listOf(
                ArtistDto(
                    artistName = "Dimmu Borgir",
                    songCount = 100,
                    albumCount = 10,
                ),
                ArtistDto(
                    artistName = "Cradle Of Filth",
                    songCount = 100,
                    albumCount = 10,
                ),
            ),
        )
    }
}
