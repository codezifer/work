package de.carsten.android.muzzic.id3.model.frame.binary

/**
 * Delivery methods of a commercial offer (COMR).
 *
 * @property code raw received-as byte.
 */
enum class ReceivedAs(val code: Byte) {

    /** Other. */
    OTHER(code = 0x00),

    /** Standard CD album with other songs. */
    CD_ALBUM(code = 0x01),

    /** Compressed audio on CD. */
    COMPRESSED_AUDIO_CD(code = 0x02),

    /** File over the Internet. */
    FILE_OVER_INTERNET(code = 0x03),

    /** Stream over the Internet. */
    STREAM_OVER_INTERNET(code = 0x04),

    /** As note sheets. */
    NOTE_SHEETS(code = 0x05),

    /** As note sheets in a book with other sheets. */
    NOTE_SHEETS_BOOK(code = 0x06),

    /** Music on other media. */
    OTHER_MEDIA(code = 0x07),

    /** Non-musical merchandise. */
    MERCHANDISE(code = 0x08),
    ;

    companion object {
        /**
         * Resolves a delivery method from its raw byte.
         *
         * @param code raw received-as byte.
         * @return matching method, defaults to [OTHER].
         */
        fun fromCode(code: Byte): ReceivedAs = entries.firstOrNull { it.code == code } ?: OTHER
    }
}
