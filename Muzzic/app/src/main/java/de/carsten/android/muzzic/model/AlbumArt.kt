package de.carsten.android.muzzic.model

/**
 * Generic album art property for genre, artist, album and playlist
 */
interface AlbumArt {
    val lastAlbumArt: String?
    val albumArts: List<String>
        get() = listOfNotNull(lastAlbumArt)
}
