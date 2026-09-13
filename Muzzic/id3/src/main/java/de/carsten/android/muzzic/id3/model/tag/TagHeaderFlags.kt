package de.carsten.android.muzzic.id3.model.tag

/**
 * Flags of the 10 byte tag header.
 *
 * v2.3 uses bits `%abc00000`, v2.4 uses `%abcd0000`. All other bits
 * must be cleared.
 *
 * @property unsynchronisation whole-tag unsynchronisation applied (v2.3) or all frames unsynchronised (v2.4).
 * @property extendedHeader an extended header follows the tag header.
 * @property experimental tag is in experimental stage.
 * @property footerPresent a footer is present at the end of the tag. Only v2.4; always false for v2.3.
 */
data class TagHeaderFlags(val unsynchronisation: Boolean = false, val extendedHeader: Boolean = false, val experimental: Boolean = false, val footerPresent: Boolean = false) {
    /**
     * Serialises the flags for the given version.
     *
     * @param version target tag version.
     * @return raw flags byte.
     */
    fun toByte(version: Id3Version): Byte {
        var value = 0
        if (unsynchronisation) value = value or UNSYNCHRONISATION_MASK
        if (extendedHeader) value = value or EXTENDED_HEADER_MASK
        if (experimental) value = value or EXPERIMENTAL_MASK
        if (footerPresent && version == Id3Version.V2_4) value = value or FOOTER_MASK
        return value.toByte()
    }

    companion object {
        private const val UNSYNCHRONISATION_MASK = 0x80
        private const val EXTENDED_HEADER_MASK = 0x40
        private const val EXPERIMENTAL_MASK = 0x20
        private const val FOOTER_MASK = 0x10

        /**
         * Parses a raw flags byte.
         *
         * Bits valid in v2.3 (`a`, `b`, `c`) sit at the same positions as
         * in v2.4, so one parsing routine covers both versions. Bit `d`
         * (footer) is only honoured for v2.4.
         *
         * @param version tag version the flags belong to.
         * @param raw raw flags byte.
         * @return parsed [TagHeaderFlags].
         */
        fun fromByte(version: Id3Version, raw: Byte): TagHeaderFlags {
            val value = raw.toInt() and 0xFF
            return TagHeaderFlags(
                unsynchronisation = value and UNSYNCHRONISATION_MASK != 0,
                extendedHeader = value and EXTENDED_HEADER_MASK != 0,
                experimental = value and EXPERIMENTAL_MASK != 0,
                footerPresent = version == Id3Version.V2_4 && value and FOOTER_MASK != 0,
            )
        }
    }
}
