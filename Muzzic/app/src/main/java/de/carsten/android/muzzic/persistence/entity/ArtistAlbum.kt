package de.carsten.android.muzzic.persistence.entity

data class ArtistAlbum(
    val artist: String,
    val album: String,
    val albumArt: String? = null
)
