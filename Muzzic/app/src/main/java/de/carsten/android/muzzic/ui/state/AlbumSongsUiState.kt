package de.carsten.android.muzzic.ui.state

import de.carsten.android.muzzic.ui.model.SongDto

data class AlbumSongsUiState(val artistName: String = "", val albumName: String = "", val songs: List<SongDto> = emptyList())
