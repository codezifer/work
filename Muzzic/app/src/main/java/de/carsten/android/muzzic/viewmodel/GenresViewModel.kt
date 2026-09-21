package de.carsten.android.muzzic.viewmodel

import androidx.annotation.OptIn
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import de.carsten.android.muzzic.persistence.repo.ArtistRepository
import de.carsten.android.muzzic.persistence.repo.GenreRepository
import de.carsten.android.muzzic.persistence.repo.PlayingQueueRepository
import de.carsten.android.muzzic.service.MediaLibraryManager
import de.carsten.android.muzzic.ui.AppDestinations.GENRE_ARGUMENT
import de.carsten.android.muzzic.ui.state.GenresUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class GenresViewModel(
    savedStateHandle: SavedStateHandle,
    private val genreRepository: GenreRepository,
    private val artistRepository: ArtistRepository,
    playingQueueRepository: PlayingQueueRepository,
    mediaLibraryManager: MediaLibraryManager,
) : AbstractViewModel(playingQueueRepository, mediaLibraryManager),
    UiStateViewModel<GenresUiState> {
    private val genreName: String? = savedStateHandle[GENRE_ARGUMENT]

    override val uiState: StateFlow<GenresUiState> = combine(
        genreRepository.getGenreInformation(),
        if (genreName != null) artistRepository.getArtistsByGenre(genreName) else flowOf(emptyList()),
    ) { genres, artists ->
        GenresUiState(
            genres = genres,
            artists = artists,
            selectedGenreName = genreName,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GenresUiState(selectedGenreName = genreName),
    )

    fun playArtist(artistName: String) {
        viewModelScope.launch {
            enqueue(artistRepository.getSongsByArtist(artistName))
        }
    }

    fun playGenre(genreName: String) {
        viewModelScope.launch {
            enqueue(genreRepository.getSongsByGenre(genreName))
        }
    }
}
