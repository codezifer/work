package de.carsten.android.muzzic.id3.model.tag

/**
 * Supported ID3v2 major versions.
 *
 * v2.3 and v2.4 share the overall tag layout but differ in size encoding
 * (plain u32 vs. synchsafe), extended header format and flag layouts.
 *
 * @property major major version byte as found after "ID3".
 * @property minor revision byte as found after the major version byte.
 */
enum class Id3Version(val major: Byte, val minor: Byte) {
    /** ID3v2.3.0. */
    V2_3(major = 3, minor = 0),

    /** ID3v2.4.0. */
    V2_4(major = 4, minor = 0),
    ;

    companion object {
        /**
         * Resolves a version from its raw header bytes.
         *
         * @param major major version byte.
         * @param minor revision byte.
         * @return matching [Id3Version] or null if unsupported.
         */
        fun from(major: Byte, minor: Byte): Id3Version? = entries.firstOrNull { it.major == major && it.minor == minor }
    }
}
