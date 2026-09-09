package de.carsten.android.muzzic.ui.state

import de.carsten.android.muzzic.ui.model.ArtistDto
import de.carsten.android.muzzic.ui.model.GenreDto

data class GenresUiState(val genres: List<GenreDto> = emptyList(), val artists: List<ArtistDto> = emptyList(), val selectedGenreName: String? = null)
