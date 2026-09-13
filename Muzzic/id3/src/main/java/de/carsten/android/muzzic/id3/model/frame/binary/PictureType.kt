package de.carsten.android.muzzic.id3.model.frame.binary

/**
 * Attached picture types (APIC).
 *
 * @property code raw picture type byte.
 */
enum class PictureType(val code: Byte) {

    /** Other. */
    OTHER(code = 0x00),

    /** 32x32 pixels file icon (PNG only). */
    FILE_ICON(code = 0x01),

    /** Other file icon. */
    OTHER_FILE_ICON(code = 0x02),

    /** Cover (front). */
    COVER_FRONT(code = 0x03),

    /** Cover (back). */
    COVER_BACK(code = 0x04),

    /** Leaflet page. */
    LEAFLET(code = 0x05),

    /** Media, e.g. label side of CD. */
    MEDIA(code = 0x06),

    /** Lead artist/lead performer/soloist. */
    LEAD_ARTIST(code = 0x07),

    /** Artist/performer. */
    ARTIST(code = 0x08),

    /** Conductor. */
    CONDUCTOR(code = 0x09),

    /** Band/Orchestra. */
    BAND(code = 0x0A),

    /** Composer. */
    COMPOSER(code = 0x0B),

    /** Lyricist/text writer. */
    LYRICIST(code = 0x0C),

    /** Recording location. */
    RECORDING_LOCATION(code = 0x0D),

    /** During recording. */
    DURING_RECORDING(code = 0x0E),

    /** During performance. */
    DURING_PERFORMANCE(code = 0x0F),

    /** Movie/video screen capture. */
    SCREEN_CAPTURE(code = 0x10),

    /** A bright coloured fish. */
    BRIGHT_FISH(code = 0x11),

    /** Illustration. */
    ILLUSTRATION(code = 0x12),

    /** Band/artist logotype. */
    BAND_LOGO(code = 0x13),

    /** Publisher/studio logotype. */
    PUBLISHER_LOGO(code = 0x14),
    ;

    companion object {
        /**
         * Resolves a picture type from its raw byte.
         *
         * @param code raw picture type byte.
         * @return matching type, defaults to [OTHER].
         */
        fun fromCode(code: Byte): PictureType = entries.firstOrNull { it.code == code } ?: OTHER
    }
}
