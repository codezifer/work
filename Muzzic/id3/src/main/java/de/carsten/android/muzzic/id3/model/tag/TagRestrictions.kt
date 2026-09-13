package de.carsten.android.muzzic.id3.model.tag

/**
 * Tag size restrictions of a v2.4 extended header (`%pp` bits).
 *
 * @property code two-bit code as stored in the restrictions byte.
 * @property maxFrames maximum frame count.
 * @property maxTagSizeBytes maximum total tag size in bytes.
 */
enum class TagSizeRestriction(val code: Int, val maxFrames: Int, val maxTagSizeBytes: Int) {
    /** No more than 128 frames and 1 MB total tag size. */
    UP_TO_128_FRAMES_1_MB(code = 0b00, maxFrames = 128, maxTagSizeBytes = 1024 * 1024),

    /** No more than 64 frames and 128 KB total tag size. */
    UP_TO_64_FRAMES_128_KB(code = 0b01, maxFrames = 64, maxTagSizeBytes = 128 * 1024),

    /** No more than 32 frames and 40 KB total tag size. */
    UP_TO_32_FRAMES_40_KB(code = 0b10, maxFrames = 32, maxTagSizeBytes = 40 * 1024),

    /** No more than 32 frames and 4 KB total tag size. */
    UP_TO_32_FRAMES_4_KB(code = 0b11, maxFrames = 32, maxTagSizeBytes = 4 * 1024),
    ;

    companion object {
        /**
         * Resolves a restriction from its two-bit code.
         *
         * @param code raw code.
         * @return matching restriction, defaults to the loosest level.
         */
        fun fromCode(code: Int): TagSizeRestriction = entries.firstOrNull { it.code == code } ?: UP_TO_128_FRAMES_1_MB
    }
}

/**
 * Text field size restrictions of a v2.4 extended header (`%rr` bits).
 *
 * Limits apply to the sum of all strings inside one text frame.
 *
 * @property code two-bit code as stored in the restrictions byte.
 * @property maxCharacters maximum characters, null means no restriction.
 */
enum class TextSizeRestriction(val code: Int, val maxCharacters: Int?) {
    /** No restrictions. */
    NONE(code = 0b00, maxCharacters = null),

    /** No string longer than 1024 characters. */
    UP_TO_1024_CHARS(code = 0b01, maxCharacters = 1024),

    /** No string longer than 128 characters. */
    UP_TO_128_CHARS(code = 0b10, maxCharacters = 128),

    /** No string longer than 30 characters. */
    UP_TO_30_CHARS(code = 0b11, maxCharacters = 30),
    ;

    companion object {
        /**
         * Resolves a restriction from its two-bit code.
         *
         * @param code raw code.
         * @return matching restriction, defaults to [NONE].
         */
        fun fromCode(code: Int): TextSizeRestriction = entries.firstOrNull { it.code == code } ?: NONE
    }
}

/**
 * Image size restrictions of a v2.4 extended header (`%tt` bits).
 *
 * @property code two-bit code as stored in the restrictions byte.
 */
enum class ImageSizeRestriction(val code: Int) {
    /** No restrictions. */
    NONE(code = 0b00),

    /** All images are 256x256 pixels or smaller. */
    UP_TO_256_PX(code = 0b01),

    /** All images are 64x64 pixels or smaller. */
    UP_TO_64_PX(code = 0b10),

    /** All images are exactly 64x64 pixels, unless required otherwise. */
    EXACTLY_64_PX(code = 0b11),
    ;

    companion object {
        /**
         * Resolves a restriction from its two-bit code.
         *
         * @param code raw code.
         * @return matching restriction, defaults to [NONE].
         */
        fun fromCode(code: Int): ImageSizeRestriction = entries.firstOrNull { it.code == code } ?: NONE
    }
}

/**
 * Artificial tag restrictions declared in a v2.4 extended header.
 *
 * Restrictions only describe how the tag was limited before encoding;
 * they never change how a tag is decoded.
 *
 * @property tagSizeRestriction frame count and total size limits (`%pp`).
 * @property textEncodingRestricted strings use only ISO-8859-1 or UTF-8 (`%q`).
 * @property textSizeRestriction per-frame string length limits (`%rr`).
 * @property imageEncodingRestricted images use only PNG or JPEG (`%s`).
 * @property imageSizeRestriction image dimension limits (`%tt`).
 */
data class TagRestrictions(
    val tagSizeRestriction: TagSizeRestriction = TagSizeRestriction.UP_TO_128_FRAMES_1_MB,
    val textEncodingRestricted: Boolean = false,
    val textSizeRestriction: TextSizeRestriction = TextSizeRestriction.NONE,
    val imageEncodingRestricted: Boolean = false,
    val imageSizeRestriction: ImageSizeRestriction = ImageSizeRestriction.NONE,
) {
    companion object {
        /**
         * Parses a raw restrictions byte (`%ppqrrstt`).
         *
         * @param raw raw restrictions byte.
         * @return parsed [TagRestrictions].
         */
        fun fromByte(raw: Byte): TagRestrictions {
            val value = raw.toInt() and 0xFF
            return TagRestrictions(
                tagSizeRestriction = TagSizeRestriction.fromCode(value shr 6 and 0b11),
                textEncodingRestricted = value and 0x20 != 0,
                textSizeRestriction = TextSizeRestriction.fromCode(value shr 3 and 0b11),
                imageEncodingRestricted = value and 0x04 != 0,
                imageSizeRestriction = ImageSizeRestriction.fromCode(value and 0b11),
            )
        }
    }
}
