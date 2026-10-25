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
import de.carsten.android.muzzic.UNKNOWN_ALBUM
import de.carsten.android.muzzic.UNKNOWN_ARTIST
import de.carsten.android.muzzic.ui.FONT_SIZE_BODY
import de.carsten.android.muzzic.ui.FONT_SIZE_LARGE_TITLE
import de.carsten.android.muzzic.ui.GLASS_CONTAINER_ALPHA
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.SPACING_LARGE
import de.carsten.android.muzzic.ui.SPACING_MEDIUM
import de.carsten.android.muzzic.ui.model.SongDto
import de.carsten.android.muzzic.ui.navigation.MusicAppState
import de.carsten.android.muzzic.ui.screens.grids.SongList
import de.carsten.android.muzzic.ui.theme.AppTheme
import de.carsten.android.muzzic.viewmodel.AlbumSongsViewModel
import de.carsten.android.muzzic.viewmodel.SelectionViewModel
import de.carsten.android.muzzic.viewmodel.states.SelectionState
import java.util.UUID
import org.koin.androidx.compose.koinViewModel

@Composable
fun AlbumSongsScreen(
    modifier: Modifier = Modifier,
    appState: MusicAppState,
    onBackClick: () -> Unit = {},
    viewModel: AlbumSongsViewModel = koinViewModel(),
    selectionViewModel: SelectionViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectionState by selectionViewModel.selectionState.collectAsStateWithLifecycle()

    AlbumSongsScreenContent(
        modifier = modifier,
        albumSongs = uiState.songs,
        albumName = uiState.albumName,
        artistName = uiState.artistName,
        selectionState = selectionState,
        onBackClick = onBackClick,
        onSongClick = { song ->
            if (selectionState.isActive) {
                selectionViewModel.toggleSongSelection(song.id)
            } else {
                viewModel.playSong(song, uiState.songs)
            }
        },
        onSongLongClick = { song ->
            selectionViewModel.toggleSongSelection(song.id)
        },
    )
}

@Composable
private fun AlbumSongsScreenContent(
    modifier: Modifier = Modifier,
    albumSongs: List<SongDto> = emptyList(),
    albumName: String = UNKNOWN_ALBUM,
    artistName: String = UNKNOWN_ARTIST,
    selectionState: SelectionState? = null,
    onBackClick: () -> Unit = {},
    onSongClick: (SongDto) -> Unit = {},
    onSongLongClick: (SongDto) -> Unit = {},
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
            Column(modifier = Modifier.padding(start = SPACING_MEDIUM)) {
                Text(
                    text = albumName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = FONT_SIZE_LARGE_TITLE,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = artistName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = FONT_SIZE_BODY,
                )
            }
        }

        SongList(
            songs = albumSongs,
            onSongClick = onSongClick,
            onSongLongClick = onSongLongClick,
            selectedSongs = selectionState?.selectedSongs ?: emptySet(),
        )
    }
}

@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = PREVIEW_DARK_MODE)
fun AlbumSongsScreenPreview() {
    AppTheme {
        AlbumSongsScreenContent(
            albumName = "Test-Album",
            artistName = "Test-Artist",
            albumSongs = listOf(
                SongDto(
                    id = UUID.randomUUID().toString(),
                    title = "Test-Song-01",
                    trackNumber = 1,
                    totalTracks = 10,
                    album = "Test-Album",
                    albumYear = 2026,
                    artist = "Test-Artist",
                ),
                SongDto(
                    id = UUID.randomUUID().toString(),
                    title = "Test-Song-02",
                    trackNumber = 2,
                    totalTracks = 10,
                    album = "Test-Album",
                    albumYear = 2026,
                    artist = "Test-Artist",
                ),
                SongDto(
                    id = UUID.randomUUID().toString(),
                    title = "Test-Song-03",
                    trackNumber = 3,
                    totalTracks = 10,
                    album = "Test-Album",
                    albumYear = 2026,
                    artist = "Test-Artist",
                ),
            ),
        )
    }
}
