package de.carsten.android.muzzic.id3

/**
 * Represents the location and size of embedded album art within a file.
 *
 * @property offset The byte offset where the image data starts.
 * @property size The size of the image data in bytes.
 * @property hashCode The byte hashCode for equals check.
 */
data class AlbumArtMetadata(val offset: Long, val size: Long, val hashCode: Int = 0) {
    /**
     * Checks if the offset and size represent a valid image location.
     */
    val isValid: Boolean get() = size > 0 && offset >= 0
}
