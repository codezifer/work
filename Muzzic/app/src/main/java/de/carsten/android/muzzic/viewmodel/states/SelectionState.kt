package de.carsten.android.muzzic.viewmodel.states

enum class Selection {
    DEFAULT,
    MARKED,
    ENQUEUED,
    PERSISTED,
}

data class SelectionState(
    val value: Selection = Selection.DEFAULT,
    val selectedArtists: Set<String> = emptySet(),
    val selectedAlbums: Set<String> = emptySet(), // "artist|album"
    val selectedSongs: Set<String> = emptySet(),
) {
    val isActive: Boolean
        get() = value != Selection.DEFAULT
}
