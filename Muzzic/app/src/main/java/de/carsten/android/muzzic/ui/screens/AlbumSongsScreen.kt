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
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.carsten.android.muzzic.ui.FONT_SIZE_BODY
import de.carsten.android.muzzic.ui.FONT_SIZE_LARGE_TITLE
import de.carsten.android.muzzic.ui.GLASS_CONTAINER_ALPHA
import de.carsten.android.muzzic.ui.SPACING_LARGE
import de.carsten.android.muzzic.ui.SPACING_MEDIUM
import de.carsten.android.muzzic.ui.navigation.MusicAppState
import de.carsten.android.muzzic.ui.screens.grids.SongList
import de.carsten.android.muzzic.ui.theme.AppTheme
import de.carsten.android.muzzic.viewmodel.AlbumSongsViewModel
import de.carsten.android.muzzic.viewmodel.SelectionViewModel
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

    AppTheme {
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
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Column(modifier = Modifier.padding(start = SPACING_MEDIUM)) {
                    Text(
                        text = uiState.albumName,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = FONT_SIZE_LARGE_TITLE,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = uiState.artistName,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = FONT_SIZE_BODY,
                    )
                }
            }

            SongList(
                songs = uiState.songs,
                onSongClick = { song ->
                    if (selectionState.isActive) {
                        selectionViewModel.toggleSongSelection(song.id)
                    } else {
                        viewModel.playSong(song, uiState.songs)
                    }
                },
                onSongLongClick = { selectionViewModel.toggleSongSelection(it.id) },
                selectedSongs = selectionState.selectedSongs,
            )
        }
    }
}
