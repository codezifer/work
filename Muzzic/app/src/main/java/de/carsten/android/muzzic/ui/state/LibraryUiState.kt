package de.carsten.android.muzzic.ui.state

import de.carsten.android.muzzic.ui.model.AlbumDto
import de.carsten.android.muzzic.ui.model.ArtistDto
import de.carsten.android.muzzic.ui.model.GenreDto
import de.carsten.android.muzzic.ui.model.PlaylistDto
import de.carsten.android.muzzic.ui.model.SongDto

data class LibraryUiState(
    val artists: List<ArtistDto> = emptyList(),
    val albums: List<AlbumDto> = emptyList(),
    val songs: List<SongDto> = emptyList(),
    val genres: List<GenreDto> = emptyList(),
    val playlists: List<PlaylistDto> = emptyList(),
)
