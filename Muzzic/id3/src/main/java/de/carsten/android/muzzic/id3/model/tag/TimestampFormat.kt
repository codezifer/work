package de.carsten.android.muzzic.id3.model.tag

/**
 * Timestamp formats used by time-based frames (ETCO, SYTC, SYLT, POSS).
 *
 * Absolute time always counts from the beginning of the audio file.
 *
 * @property code raw format byte as stored in the frame.
 */
enum class TimestampFormat(val code: Byte) {
    /** Absolute time in MPEG frames, 32 bit. */
    MPEG_FRAMES(code = 0x01),

    /** Absolute time in milliseconds, 32 bit. */
    MILLISECONDS(code = 0x02),
    ;

    companion object {
        /**
         * Resolves a format from its raw byte.
         *
         * @param code raw format byte.
         * @return matching [TimestampFormat] or null for unknown codes.
         */
        fun fromCode(code: Byte): TimestampFormat? = entries.firstOrNull { it.code == code }
    }
}
