package de.carsten.android.muzzic.ui.state

import de.carsten.android.muzzic.ui.model.AlbumDto

data class ArtistAlbumsUiState(val artistName: String = "", val albums: List<AlbumDto> = emptyList())
