package de.carsten.android.muzzic.ui.screens.grids

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.carsten.android.muzzic.ui.screens.cards.SongListItem
import de.carsten.android.muzzic.viewmodel.LibraryViewModel

@Composable
fun SongList(viewModel: LibraryViewModel) {
    val songs by viewModel.songs.observeAsState(emptyList())

    LazyColumn(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(songs) { song ->
            SongListItem(song)
        }
    }
}

