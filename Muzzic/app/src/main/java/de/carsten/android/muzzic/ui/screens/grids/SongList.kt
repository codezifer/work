package de.carsten.android.muzzic.ui.screens.grids

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.carsten.android.muzzic.persistence.entity.Song
import de.carsten.android.muzzic.ui.screens.cards.SongListItem
import de.carsten.android.muzzic.viewmodel.LibraryViewModel
import org.koin.androidx.compose.koinViewModel
import java.time.Instant

@Composable
fun SongList(viewModel: LibraryViewModel = koinViewModel()) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    SongListContent(songs)
}

@Composable
fun SongListContent(songs: List<Song>) {
    LazyColumn(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(songs) { song ->
            SongListItem(song)
        }
    }
}

@Composable
@Preview
fun SongListPreview() {
    SongListContent(
        listOf(
            Song(
                title = "This is just a Test",
                album = "Test-Album",
                artist = "Test-Artist",
                duration = 3 * 60 * 1000,
                genre = "Alternative",
                lastPlayed = Instant.now(),
                playCount = 3,
                rating = 5,
                totalTracks = 10,
                trackNumber = 3,
            ),
            Song(
                title = "This is just a Test",
                album = "Test-Album",
                artist = "Test-Artist",
                duration = 3 * 60 * 1000,
                genre = "Alternative",
                lastPlayed = Instant.now(),
                playCount = 2,
                rating = 3,
                totalTracks = 10,
                trackNumber = 4,
            )
        )
    )
}
