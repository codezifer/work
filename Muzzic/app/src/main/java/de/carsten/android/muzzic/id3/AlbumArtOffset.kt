package de.carsten.android.muzzic.id3

/**
 * Represents the location and size of embedded album art within a file.
 *
 * @property offset The byte offset where the image data starts.
 * @property size The size of the image data in bytes.
 */
data class AlbumArtOffset(val offset: Long, val size: Long) {
    /**
     * Checks if the offset and size represent a valid image location.
     */
    val isValid: Boolean get() = size > 0 && offset >= 0
}
