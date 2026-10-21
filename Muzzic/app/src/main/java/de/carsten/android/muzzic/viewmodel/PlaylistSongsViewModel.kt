package de.carsten.android.muzzic.viewmodel

import androidx.annotation.OptIn
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import de.carsten.android.muzzic.persistence.repo.PlayingQueueRepository
import de.carsten.android.muzzic.persistence.repo.PlaylistRepository
import de.carsten.android.muzzic.service.MediaLibraryManager
import de.carsten.android.muzzic.ui.AppDestinations.PLAYLIST_ID_ARGUMENT
import de.carsten.android.muzzic.ui.model.SongDto
import de.carsten.android.muzzic.ui.state.PlaylistSongsUiState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * ViewModel for the playlist detail screen.
 *
 * Observes the playlist name and its songs, and starts playback of a selected song
 * within the playlist context.
 *
 * @param savedStateHandle provides the [PLAYLIST_ID_ARGUMENT] from the navigation route.
 * @param playlistRepository source of the playlist name and songs.
 * @param playingQueueRepository replaced with the playlist when a song starts playing.
 * @param mediaLibraryManager starts playback in the media session.
 */
@OptIn(UnstableApi::class)
class PlaylistSongsViewModel(
    savedStateHandle: SavedStateHandle,
    private val playlistRepository: PlaylistRepository,
    playingQueueRepository: PlayingQueueRepository,
    mediaLibraryManager: MediaLibraryManager,
) : AbstractViewModel(playingQueueRepository, mediaLibraryManager),
    UiStateViewModel<PlaylistSongsUiState> {
    private val playlistId: String = checkNotNull(savedStateHandle[PLAYLIST_ID_ARGUMENT])

    override val uiState: StateFlow<PlaylistSongsUiState> =
        combine(
            playlistRepository.observePlaylistName(playlistId),
            playlistRepository.observeSongsInPlaylist(playlistId),
        ) { playlistName, songs ->
            PlaylistSongsUiState(
                playlistId = playlistId,
                playlistName = playlistName.orEmpty(),
                songs = songs,
                totalDurationMs = songs.sumOf { it.duration },
            )
        }.asUiState(viewModelScope, PlaylistSongsUiState(playlistId))

    /**
     * Replaces the playing queue with the playlist and starts at the given song.
     *
     * @param song the song the user selected.
     * @param allSongs all songs of the playlist, used as the new queue.
     */
    fun playSong(song: SongDto, allSongs: List<SongDto>) {
        val startIndex = allSongs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        viewModelScope.launch {
            enqueue(allSongs, startIndex)
        }
    }
}
