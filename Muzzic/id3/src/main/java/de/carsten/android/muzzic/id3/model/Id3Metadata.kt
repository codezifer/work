package de.carsten.android.muzzic.id3.model

data class Id3Metadata(
    val title: String? = null,
    val track: Int? = null,
    val totalTracks: Int? = null,
    val artist: String? = null,
    val album: String? = null,
    val albumYear: Int? = null,
    val albumArt: AlbumArtMetadata? = null,
    val genre: String? = null,
    val rating: Int? = null,
    val playCount: Int? = null,
    val duration: Long? = null,
    val comment: String? = null,
)
