package de.carsten.android.muzzic.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.carsten.android.muzzic.persistence.repo.ArtistRepository
import de.carsten.android.muzzic.persistence.repo.GenreRepository
import de.carsten.android.muzzic.ui.AppDestinations.GENRE_ARGUMENT
import de.carsten.android.muzzic.ui.model.ArtistDto
import de.carsten.android.muzzic.ui.model.GenreDto
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class GenresViewModel(
    savedStateHandle: SavedStateHandle,
    private val genreRepository: GenreRepository,
    private val artistRepository: ArtistRepository,
) : ViewModel() {
    val genreName: String = checkNotNull(savedStateHandle[GENRE_ARGUMENT])

    val genres: StateFlow<List<GenreDto>> = genreRepository.getGenreInformation().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    val artists: StateFlow<List<ArtistDto>> = artistRepository.getArtistsByGenre(genreName).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

}
