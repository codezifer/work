package de.carsten.android.muzzic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.UNKNOWN_ARTIST
import de.carsten.android.muzzic.ui.FONT_SIZE_LARGE_TITLE
import de.carsten.android.muzzic.ui.GLASS_CONTAINER_ALPHA
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.SPACING_LARGE
import de.carsten.android.muzzic.ui.SPACING_MEDIUM
import de.carsten.android.muzzic.ui.model.AlbumDto
import de.carsten.android.muzzic.ui.model.ColorSource
import de.carsten.android.muzzic.ui.model.FastScrollMode
import de.carsten.android.muzzic.ui.model.composableColorSource
import de.carsten.android.muzzic.ui.navigation.MusicAppState
import de.carsten.android.muzzic.ui.screens.grids.AlbumGrid
import de.carsten.android.muzzic.ui.theme.AppTheme
import de.carsten.android.muzzic.viewmodel.ArtistAlbumsViewModel
import de.carsten.android.muzzic.viewmodel.SelectionViewModel
import de.carsten.android.muzzic.viewmodel.states.SelectionState
import org.koin.androidx.compose.koinViewModel

@Composable
fun ArtistAlbumsScreen(
    modifier: Modifier = Modifier,
    appState: MusicAppState,
    colorSource: ColorSource = composableColorSource(),
    fastScrollMode: FastScrollMode = FastScrollMode.FAST_SCROLLER,
    onAlbumClick: (String, String) -> Unit = { _, _ -> },
    onBackClick: () -> Unit = {},
    artistAlbumsViewModel: ArtistAlbumsViewModel = koinViewModel(),
    selectionViewModel: SelectionViewModel,
) {
    val uiState by artistAlbumsViewModel.uiState.collectAsStateWithLifecycle()
    val selectionState by selectionViewModel.selectionState.collectAsStateWithLifecycle()

    ArtistAlbumsScreenContent(
        modifier = modifier,
        albums = uiState.albums,
        artistName = uiState.artistName,
        selectionState = selectionState,
        onBackClick = onBackClick,
        onAlbumClick = { artist, album ->
            if (selectionState.isActive) {
                selectionViewModel.toggleAlbumSelection(artist, album)
            } else {
                onAlbumClick(artist, album)
            }
        },
        onAlbumPlayClick = artistAlbumsViewModel::playAlbum,
        onAlbumLongClick = selectionViewModel::toggleAlbumSelection,
        colorSource = colorSource,
        fastScrollMode = fastScrollMode,
    )
}

@Composable
private fun ArtistAlbumsScreenContent(
    modifier: Modifier = Modifier,
    albums: List<AlbumDto> = emptyList(),
    artistName: String = UNKNOWN_ARTIST,
    selectionState: SelectionState? = null,
    onBackClick: () -> Unit = {},
    onAlbumPlayClick: (String, String) -> Unit = { _, _ -> },
    onAlbumClick: (String, String) -> Unit = { _, _ -> },
    onAlbumLongClick: (String, String) -> Unit = { _, _ -> },
    colorSource: ColorSource = composableColorSource(),
    fastScrollMode: FastScrollMode = FastScrollMode.FAST_SCROLLER,
) {
    Column(
        modifier =
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = GLASS_CONTAINER_ALPHA)),
    ) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(SPACING_LARGE),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = artistName,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = FONT_SIZE_LARGE_TITLE,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = SPACING_MEDIUM),
            )
        }

        AlbumGrid(
            albums = albums,
            onAlbumClick = onAlbumClick,
            onAlbumPlayClick = onAlbumPlayClick,
            onAlbumLongClick = onAlbumLongClick,
            selectedAlbums = selectionState?.selectedAlbums ?: emptySet(),
            colorSource = colorSource,
            fastScrollMode = fastScrollMode,
        )
    }
}

@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = PREVIEW_DARK_MODE)
fun ArtistAlbumsScreenPreview() {
    AppTheme {
        ArtistAlbumsScreenContent(
            artistName = "Test-Artist",
            albums = listOf(
                AlbumDto(
                    albumName = "Test-Album-01",
                    artistName = "Test-Artist",
                    albumYear = 2024,
                    songCount = 10,
                    albumDuration = 60 * 60 * 1000L,
                ),
                AlbumDto(
                    albumName = "Test-Album-02",
                    artistName = "Test-Artist",
                    albumYear = 2025,
                    songCount = 10,
                    albumDuration = 60 * 60 * 1000L,
                ),
                AlbumDto(
                    albumName = "Test-Album-03",
                    artistName = "Test-Artist",
                    albumYear = 2026,
                    songCount = 10,
                    albumDuration = 60 * 60 * 1000L,
                ),
            ),
        )
    }
}
