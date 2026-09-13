package de.carsten.android.muzzic.id3.model.tag

/**
 * Text encodings usable inside ID3v2 frames.
 *
 * v2.3 only supports [ISO_8859_1] and [UTF_16]; [UTF_16BE] and [UTF_8]
 * were added with v2.4.
 *
 * @property code encoding description byte as stored in the frame.
 * @property terminator bytes terminating a string in this encoding.
 * @property charsetName JVM charset name used for decoding.
 */
enum class TextEncoding(val code: Byte, val terminator: ByteArray, val charsetName: String) {
    /** ISO-8859-1, terminated with a single $00. */
    ISO_8859_1(code = 0x00, terminator = byteArrayOf(0x00), charsetName = "ISO-8859-1"),

    /** UTF-16 with BOM, terminated with $00 00. */
    UTF_16(code = 0x01, terminator = byteArrayOf(0x00, 0x00), charsetName = "UTF-16"),

    /** UTF-16BE without BOM, terminated with $00 00. Only v2.4. */
    UTF_16BE(code = 0x02, terminator = byteArrayOf(0x00, 0x00), charsetName = "UTF-16BE"),

    /** UTF-8, terminated with a single $00. Only v2.4. */
    UTF_8(code = 0x03, terminator = byteArrayOf(0x00), charsetName = "UTF-8"),
    ;

    /**
     * Checks whether this encoding is valid for the given tag version.
     *
     * @param version tag version to check against.
     * @return true if the encoding may appear in a tag of that version.
     */
    fun isSupportedBy(version: Id3Version): Boolean = when (version) {
        Id3Version.V2_3 -> this == ISO_8859_1 || this == UTF_16
        Id3Version.V2_4 -> true
    }

    companion object {
        /**
         * Resolves an encoding from its raw description byte.
         *
         * @param code raw encoding byte.
         * @return matching [TextEncoding] or null for unknown codes.
         */
        fun fromCode(code: Byte): TextEncoding? = entries.firstOrNull { it.code == code }
    }
}
