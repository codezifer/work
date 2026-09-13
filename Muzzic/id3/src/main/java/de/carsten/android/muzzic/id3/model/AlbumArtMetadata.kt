package de.carsten.android.muzzic.id3.model

/**
 * Metadata for album art in mp3 file.
 *
 * The offset is the absolute stream position of the first picture byte,
 * counted from the tag header: for the usual prepended tag this equals
 * the file offset, so consumers can `skip(offset)` and read `length`
 * bytes. When unsynchronisation was reversed while parsing, the stored
 * bytes are the plain picture data while the offset still points into
 * the unsynchronised stream layout.
 *
 * @param offset starting position of album art bytes, `0` means unknown.
 * @param length bytes length of album art.
 * @param mimeType optional image mime type e.g. image/jpg or image/png.
 * @param hashCode optional album art bytes hash code.
 */
data class AlbumArtMetadata(val offset: Int, val length: Int, val mimeType: String? = null, val hashCode: Int? = null) {
    /**
     * Checks if the offset and size represent a valid image location.
     */
    fun isValid() = length > 0 && offset > 0
}
