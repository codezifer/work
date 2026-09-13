package de.carsten.android.muzzic.id3.model.frame.language

/**
 * Synchronised lyrics content types.
 *
 * @property code raw content type byte.
 */
enum class SyltContentType(val code: Byte) {

    /** Other content. */
    OTHER(code = 0x00),

    /** Lyrics. */
    LYRICS(code = 0x01),

    /** Text transcription. */
    TEXT_TRANSCRIPTION(code = 0x02),

    /** Movement/part name, e.g. "Adagio". */
    MOVEMENT_NAME(code = 0x03),

    /** Events, e.g. stage directions. */
    EVENTS(code = 0x04),

    /** Chords, e.g. "Bb F Fsus". */
    CHORD(code = 0x05),

    /** Trivia or pop-up information. */
    TRIVIA(code = 0x06),

    /** URLs to webpages. New in v2.4. */
    WEBPAGE_URL(code = 0x07),

    /** URLs to images. New in v2.4. */
    IMAGE_URL(code = 0x08),
    ;

    companion object {
        /**
         * Resolves a content type from its raw byte.
         *
         * @param code raw content type byte.
         * @return matching type, defaults to [OTHER].
         */
        fun fromCode(code: Byte): SyltContentType = entries.firstOrNull { it.code == code } ?: OTHER
    }
}
