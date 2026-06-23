package de.carsten.android.muzzic.viewmodel

import androidx.annotation.OptIn
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import de.carsten.android.muzzic.persistence.repo.ArtistRepository
import de.carsten.android.muzzic.persistence.repo.GenreRepository
import de.carsten.android.muzzic.persistence.repo.PlayingQueueRepository
import de.carsten.android.muzzic.service.MediaLibraryManager
import de.carsten.android.muzzic.ui.AppDestinations.GENRE_ARGUMENT
import de.carsten.android.muzzic.ui.model.ArtistDto
import de.carsten.android.muzzic.ui.model.GenreDto
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class GenresViewModel(
    savedStateHandle: SavedStateHandle,
    private val genreRepository: GenreRepository,
    private val artistRepository: ArtistRepository,
    private val playingQueueRepository: PlayingQueueRepository,
    private val mediaLibraryManager: MediaLibraryManager,
) : ViewModel() {
    val genreName: String? = savedStateHandle[GENRE_ARGUMENT]

    val genres: StateFlow<List<GenreDto>> =
        genreRepository.getGenreInformation().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    val artists: StateFlow<List<ArtistDto>> =
        (
            genreName?.let {
                artistRepository.getArtistsByGenre(it)
            } ?: flowOf(emptyList())
            ).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    fun playArtist(artistName: String) {
        viewModelScope.launch {
            val songs = artistRepository.getSongsByArtist(artistName)
            if (songs.isNotEmpty()) {
                val mediaItems = songs.mapIndexed { index, song ->
                    song.toMediaItem().buildUpon()
                        .setMediaMetadata(
                            song.toMediaItem().mediaMetadata.buildUpon()
                                .setExtras(
                                    (song.toMediaItem().mediaMetadata.extras ?: android.os.Bundle()).apply {
                                        putInt("queuePosition", index)
                                        putString("songId", song.id)
                                    },
                                ).build(),
                        ).build()
                }
                playingQueueRepository.clear()
                playingQueueRepository.addSongs(mediaItems)
                mediaLibraryManager.playPlaylist(mediaItems, 0)
            }
        }
    }
}
