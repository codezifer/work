package de.carsten.android.muzzic.id3

/**
 * Data class to hold metadata fields that require specialized extraction logic.
 *
 * @property year The parsed album year.
 * @property rating The song rating (usually 0-255 in WMP format).
 * @property playCount The number of times the song has been played.
 * @property trackNumber The track number of the song.
 * @property totalTracks The total number of tracks in the album.
 */
data class ExtendedMetadata(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val genre: String? = null,
    val duration: Long = 0L,
    val year: Int = -1,
    val rating: Int = 0,
    val playCount: Int = 0,
    val trackNumber: Int = -1,
    val totalTracks: Int = -1,
)
