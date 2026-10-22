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
import de.carsten.android.muzzic.DOT
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.ui.FONT_SIZE_BODY
import de.carsten.android.muzzic.ui.FONT_SIZE_LARGE_TITLE
import de.carsten.android.muzzic.ui.GLASS_CONTAINER_ALPHA
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.SPACING_LARGE
import de.carsten.android.muzzic.ui.SPACING_MEDIUM
import de.carsten.android.muzzic.ui.model.ColorSource
import de.carsten.android.muzzic.ui.model.SongDto
import de.carsten.android.muzzic.ui.model.composableColorSource
import de.carsten.android.muzzic.ui.screens.grids.SongList
import de.carsten.android.muzzic.ui.theme.AppTheme
import de.carsten.android.muzzic.ui.utils.formatDurationLong
import de.carsten.android.muzzic.viewmodel.PlaylistSongsViewModel
import de.carsten.android.muzzic.viewmodel.SelectionViewModel
import java.time.Instant
import kotlin.uuid.Uuid
import org.koin.androidx.compose.koinViewModel

/**
 * Shows all songs of a playlist.
 *
 * A click on a song starts playback within the playlist context; a long click
 * toggles the song selection.
 *
 * @param modifier the [Modifier] for the screen.
 * @param onBackClick callback triggered when the back button is clicked.
 * @param viewModel provides the playlist songs.
 * @param selectionViewModel handles multi-selection of songs.
 * @param colorSource the color source holder for accent- and content colors.
 */
@Composable
fun PlaylistSongsScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    viewModel: PlaylistSongsViewModel = koinViewModel(),
    selectionViewModel: SelectionViewModel,
    colorSource: ColorSource = composableColorSource(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectionState by selectionViewModel.selectionState.collectAsStateWithLifecycle()

    PlaylistSongsScreenContent(
        modifier = modifier,
        playlistName = uiState.playlistName,
        songs = uiState.songs,
        totalDurationMs = uiState.totalDurationMs,
        colorSource = colorSource,
        onSongClick = { song ->
            if (selectionState.isActive) {
                selectionViewModel.toggleSongSelection(song.id)
            } else {
                viewModel.playSong(song, uiState.songs)
            }
        },
        onSongLongClick = { selectionViewModel.toggleSongSelection(it.id) },
        selectedSongs = selectionState.selectedSongs,
        onBackClick = onBackClick,
    )
}

/**
 * Stateless content of the playlist detail screen.
 *
 * @param modifier the [Modifier] for the content.
 * @param playlistName name of the playlist.
 * @param songs songs of the playlist in playlist order.
 * @param totalDurationMs summed duration of all [songs] in milliseconds.
 * @param onSongClick callback triggered when a song is clicked.
 * @param onSongLongClick callback triggered when a song is long-clicked.
 * @param selectedSongs ids of the currently selected songs.
 * @param onBackClick callback triggered when the back button is clicked.
 * @param colorSource the color source holder for accent- and content colors.
 */
@Composable
fun PlaylistSongsScreenContent(
    modifier: Modifier = Modifier,
    playlistName: String,
    songs: List<SongDto>,
    totalDurationMs: Long,
    onSongClick: (SongDto) -> Unit = {},
    onSongLongClick: (SongDto) -> Unit = {},
    selectedSongs: Set<String> = emptySet(),
    onBackClick: () -> Unit = {},
    colorSource: ColorSource = composableColorSource(),
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
                    text = playlistName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = FONT_SIZE_LARGE_TITLE,
                    fontWeight = FontWeight.Bold,
                )
                if (songs.isNotEmpty()) {
                    Text(
                        text = "${stringResource(R.string.playlist_songs_count, songs.size)} $DOT ${formatDurationLong(totalDurationMs)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = FONT_SIZE_BODY,
                    )
                }
            }
        }

        SongList(
            songs = songs,
            onSongClick = onSongClick,
            onSongLongClick = onSongLongClick,
            selectedSongs = selectedSongs,
            colorSource = colorSource,
        )
    }
}

private val previewSongs = listOf(
    SongDto(
        id = Uuid.random().toString(),
        title = "This is just a Test 1",
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
    SongDto(
        id = Uuid.random().toString(),
        title = "This is just a Test 2",
        album = "Test-Album",
        artist = "Test-Artist",
        duration = 4 * 60 * 1000,
        genre = "Alternative",
        lastPlayed = Instant.now(),
        playCount = 2,
        rating = 3,
        totalTracks = 10,
        trackNumber = 4,
    ),
)

@Composable
@Preview(showBackground = true, name = "Light Mode")
@Preview(uiMode = PREVIEW_DARK_MODE, showBackground = true, name = "Dark Mode")
fun PlaylistSongsScreenPreview() {
    AppTheme {
        PlaylistSongsScreenContent(
            playlistName = "Rock Anthems",
            songs = previewSongs,
            totalDurationMs = previewSongs.sumOf { it.duration },
        )
    }
}
