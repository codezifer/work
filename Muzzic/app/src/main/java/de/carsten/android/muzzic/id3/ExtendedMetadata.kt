package de.carsten.android.muzzic.id3

/**
 * Data class to hold metadata fields that require specialized extraction logic.
 *
 * @property year The parsed album year.
 * @property rating The song rating (usually 0-255 in WMP format).
 * @property playCount The number of times the song has been played.
 */
data class ExtendedMetadata(
    val year: Int = -1,
    val rating: Int = 0,
    val playCount: Int = 0
)
