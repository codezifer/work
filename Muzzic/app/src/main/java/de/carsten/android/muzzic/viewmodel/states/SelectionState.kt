package de.carsten.android.muzzic.viewmodel.states

data class SelectionState(
    val isActive: Boolean = false,
    val selectedArtists: Set<String> = emptySet(),
    val selectedAlbums: Set<String> = emptySet(), // "artist|album"
    val selectedSongs: Set<String> = emptySet(),
)
