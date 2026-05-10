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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.carsten.android.muzzic.ui.screens.grids.AlbumGrid
import de.carsten.android.muzzic.ui.theme.AppTheme
import de.carsten.android.muzzic.viewmodel.ArtistAlbumsViewModel
import de.carsten.android.muzzic.viewmodel.SelectionViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun ArtistAlbumsScreen(
    modifier: Modifier = Modifier,
    onAlbumClick: (String, String) -> Unit = { _, _ -> },
    onBackClick: () -> Unit = {},
    viewModel: ArtistAlbumsViewModel = koinViewModel(),
    selectionViewModel: SelectionViewModel,
) {
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val selectionState by selectionViewModel.selectionState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = viewModel.artistName,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        AlbumGrid(
            albums = albums,
            onAlbumClick = { artist, album ->
                if (selectionState.isActive) {
                    selectionViewModel.toggleAlbumSelection(artist, album)
                } else {
                    onAlbumClick(artist, album)
                }
            },
            onAlbumLongClick = { artist, album -> selectionViewModel.toggleAlbumSelection(artist, album) },
            selectedAlbums = selectionState.selectedAlbums,
        )
    }
}
