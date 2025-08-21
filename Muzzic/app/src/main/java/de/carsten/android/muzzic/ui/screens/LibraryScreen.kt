package de.carsten.android.muzzic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.ui.backgroundColor
import de.carsten.android.muzzic.ui.containerColor
import de.carsten.android.muzzic.ui.primaryColor
import de.carsten.android.muzzic.ui.screens.grids.AlbumGrid
import de.carsten.android.muzzic.ui.screens.grids.ArtistGrid
import de.carsten.android.muzzic.viewmodel.LibraryViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun LibraryScreen(modifier: Modifier = Modifier) {
    val viewModel: LibraryViewModel = koinViewModel()
    var selectedFilter by remember { mutableStateOf("artist") }
    val filters = listOf(
        "artist" to stringResource(R.string.artists),
        "album" to stringResource(R.string.albums),
        "song" to stringResource(R.string.songs),
        "genre" to stringResource(R.string.genres),
        "playlist" to stringResource(R.string.playlists)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {

        // Header
        Text(
            text = stringResource(R.string.library),
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filters) { (key, label) ->
                FilterChip(
                    selected = selectedFilter == key,
                    onClick = { selectedFilter = key },
                    label = { Text(text = label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = primaryColor,
                        selectedLabelColor = Color.White,
                        containerColor = containerColor,
                        labelColor = Color.Gray
                    )
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Content based on filters
    when (selectedFilter) {
        "artist" -> ArtistGrid(viewModel)
        "album" -> AlbumGrid(viewModel)
        "song" -> SongList(viewModel)
        "genre" -> GenreGrid(viewModel)
        "playlist" -> PlaylisttGrid(viewModel)
    }
}

@Preview
@Composable
fun LibraryScreenPreview() {
    LibraryScreen(Modifier.padding(2.dp))
}
