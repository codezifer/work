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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.persistence.entity.Song
import de.carsten.android.muzzic.ui.AppDestinations.ALBUM
import de.carsten.android.muzzic.ui.AppDestinations.ARTIST
import de.carsten.android.muzzic.ui.AppDestinations.GENRE
import de.carsten.android.muzzic.ui.AppDestinations.PLAYLIST
import de.carsten.android.muzzic.ui.AppDestinations.SONG
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.model.AlbumDto
import de.carsten.android.muzzic.ui.model.ArtistDto
import de.carsten.android.muzzic.ui.model.GenreDto
import de.carsten.android.muzzic.ui.model.PlaylistDto
import de.carsten.android.muzzic.ui.navigation.MusicAppState
import de.carsten.android.muzzic.ui.screens.grids.AlbumGrid
import de.carsten.android.muzzic.ui.screens.grids.ArtistGrid
import de.carsten.android.muzzic.ui.screens.grids.GenreGrid
import de.carsten.android.muzzic.ui.screens.grids.PlaylistGridContent
import de.carsten.android.muzzic.ui.screens.grids.SongList
import de.carsten.android.muzzic.ui.theme.AppTheme
import de.carsten.android.muzzic.viewmodel.LibraryViewModel
import de.carsten.android.muzzic.viewmodel.SelectionViewModel
import org.koin.androidx.compose.koinViewModel
import java.time.Instant

@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    appState: MusicAppState,
    onArtistClick: (String) -> Unit = {},
    onAlbumClick: (String, String) -> Unit = { _, _ -> },
    viewModel: LibraryViewModel = koinViewModel(),
    selectionViewModel: SelectionViewModel,
) {
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val genres by viewModel.genres.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val selectionState by selectionViewModel.selectionState.collectAsStateWithLifecycle()

    val filters =
        listOf(
            ARTIST to stringResource(R.string.artists),
            ALBUM to stringResource(R.string.albums),
            SONG to stringResource(R.string.songs),
            GENRE to stringResource(R.string.genres),
            PLAYLIST to stringResource(R.string.playlists),
        )

    LibraryScreenContent(
        modifier = modifier,
        filters = filters,
        artists = artists,
        albums = albums,
        songs = songs,
        genres = genres,
        playlists = playlists,
        onArtistClick = { artist ->
            if (selectionState.isActive) {
                selectionViewModel.toggleArtistSelection(artist)
            } else {
                onArtistClick(artist)
            }
        },
        onArtistLongClick = { selectionViewModel.toggleArtistSelection(it) },
        onAlbumClick = { artist, album ->
            if (selectionState.isActive) {
                selectionViewModel.toggleAlbumSelection(artist, album)
            } else {
                onAlbumClick(artist, album)
            }
        },
        onAlbumLongClick = { artist, album -> selectionViewModel.toggleAlbumSelection(artist, album) },
        onSongClick = { song ->
            if (selectionState.isActive) {
                selectionViewModel.toggleSongSelection(song.id)
            } else {
                viewModel.playSong(song)
            }
        },
        onSongLongClick = { selectionViewModel.toggleSongSelection(it.id) },
        selectedArtists = selectionState.selectedArtists,
        selectedAlbums = selectionState.selectedAlbums,
        selectedSongs = selectionState.selectedSongs,
    )
}

@Composable
fun LibraryScreenContent(
    modifier: Modifier = Modifier,
    filters: List<Pair<String, String>>,
    artists: List<ArtistDto>,
    albums: List<AlbumDto>,
    songs: List<Song>,
    genres: List<GenreDto>,
    playlists: List<PlaylistDto>,
    onArtistClick: (String) -> Unit = {},
    onArtistLongClick: (String) -> Unit = {},
    onAlbumClick: (String, String) -> Unit = { _, _ -> },
    onAlbumLongClick: (String, String) -> Unit = { _, _ -> },
    onSongClick: (Song) -> Unit = {},
    onSongLongClick: (Song) -> Unit = {},
    selectedArtists: Set<String> = emptySet(),
    selectedAlbums: Set<String> = emptySet(),
    selectedSongs: Set<String> = emptySet(),
) {
    var selectedFilter by remember { mutableStateOf(ARTIST) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Header
        Text(
            text = stringResource(R.string.library),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp),
        )

        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(filters) { (key, label) ->
                FilterChip(
                    selected = selectedFilter == key,
                    onClick = { selectedFilter = key },
                    label = { Text(text = label) },
                    colors =
                        FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        val contentModifier = Modifier.weight(1f)

        // Content based on filters
        when (selectedFilter) {
            ARTIST -> ArtistGrid(
                modifier = contentModifier,
                artists = artists,
                onArtistClick = onArtistClick,
                onArtistLongClick = onArtistLongClick,
                selectedArtists = selectedArtists,
            )

            ALBUM -> AlbumGrid(
                albums = albums,
                onAlbumClick = onAlbumClick,
                onAlbumLongClick = onAlbumLongClick,
                selectedAlbums = selectedAlbums,
                modifier = contentModifier,
            )

            SONG -> SongList(
                songs = songs,
                onSongClick = onSongClick,
                onSongLongClick = onSongLongClick,
                selectedSongs = selectedSongs,
                modifier = contentModifier,
            )

            GENRE -> GenreGrid(genres, contentModifier)
            PLAYLIST -> PlaylistGridContent(playlists, contentModifier)
        }
    }
}

@Composable
@Preview
@Preview(uiMode = PREVIEW_DARK_MODE)
fun LibraryScreenPreview() {
    AppTheme {
        LibraryScreenContent(
            filters =
            listOf(
                ARTIST to "Artists",
                ALBUM to "Albums",
                SONG to "Songs",
                GENRE to "Genres",
                PLAYLIST to "Playlists",
            ),
            artists =
            listOf(
                ArtistDto("Cradle Of Filth", 2, 13),
                ArtistDto("Dimmu Borgir", 1, 10),
                ArtistDto("Interpol", 1, 7),
                ArtistDto("Jimmy Eat World", 1, 10),
                ArtistDto("Marduk", 1, 23),
                ArtistDto("Marilyn Masnon", 1, 21),
            ),
            albums =
            listOf(
                AlbumDto(
                    artistName = "Dimmu Borgir",
                    albumName = "Enthrone Darkness Triumphant",
                    albumYear = 1997,
                    songCount = 21,
                    albumDuration = 90 * 60 * 1000L,
                ),
                AlbumDto(
                    artistName = "Jimmy Eat World",
                    albumName = "Bleed American",
                    albumYear = 2001,
                    songCount = 12,
                    albumDuration = 45 * 60 * 1000L,
                ),

            ),
            songs =
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
                ),
            ),
            genres =
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
            ),
            playlists =
            listOf(
                PlaylistDto(
                    playlistId = "1",
                    playlistName = "TopAlternative",
                    playlistIsAutoGenerated = true,
                    playlistGenre = "Alternative",
                    artistCount = 25,
                    albumCount = 40,
                    songCount = 100,
                    playlistDuration = 5 * 60 * 60 * 1000L,
                ),
            ),
            onArtistClick = {},
            onArtistLongClick = {},
            onAlbumClick = { _, _ -> },
            onAlbumLongClick = { _, _ -> },
            onSongClick = {},
            onSongLongClick = {},
            selectedArtists = emptySet(),
            selectedAlbums = emptySet(),
            selectedSongs = emptySet(),
        )
    }
}
