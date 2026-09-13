package de.carsten.android.muzzic.id3.model.frame

/**
 * Format flags of the frame header (second flag byte).
 *
 * v2.3 (`%ijk00000`) and v2.4 (`%0h00kmnp`) define incompatible layouts,
 * hence the sealed hierarchy. Set format flags may append extra fields
 * to the frame header in flag order; those bytes count towards the frame
 * size but are never encrypted or compressed.
 */
sealed interface FrameFormatFlags {
    /**
     * v2.3 format flags.
     *
     * @property compression frame is zlib compressed, 4 decompressed-size bytes follow the header.
     * @property encryption frame is encrypted, 1 method byte follows (see ENCR).
     * @property grouping frame belongs to a group, 1 group identifier byte follows.
     */
    data class V23(val compression: Boolean = false, val encryption: Boolean = false, val grouping: Boolean = false) : FrameFormatFlags {
        companion object {
            private const val COMPRESSION_MASK = 0x80
            private const val ENCRYPTION_MASK = 0x40
            private const val GROUPING_MASK = 0x20

            /**
             * Parses the second v2.3 flag byte.
             *
             * @param raw raw flag byte.
             * @return parsed flags.
             */
            fun fromByte(raw: Byte): V23 {
                val value = raw.toInt() and 0xFF
                return V23(
                    compression = value and COMPRESSION_MASK != 0,
                    encryption = value and ENCRYPTION_MASK != 0,
                    grouping = value and GROUPING_MASK != 0,
                )
            }
        }
    }

    /**
     * v2.4 format flags.
     *
     * @property grouping frame belongs to a group, 1 group identifier byte is added.
     * @property compression frame is zlib deflate compressed, requires [dataLengthIndicator].
     * @property encryption frame is encrypted, 1 method byte is added (see ENCR).
     * @property unsynchronisation frame body was unsynchronised.
     * @property dataLengthIndicator a 4 byte synchsafe indicator is added holding the
     * size the frame length field would have with all format flags cleared.
     */
    data class V24(
        val grouping: Boolean = false,
        val compression: Boolean = false,
        val encryption: Boolean = false,
        val unsynchronisation: Boolean = false,
        val dataLengthIndicator: Boolean = false,
    ) : FrameFormatFlags {
        companion object {
            private const val GROUPING_MASK = 0x40
            private const val COMPRESSION_MASK = 0x08
            private const val ENCRYPTION_MASK = 0x04
            private const val UNSYNCHRONISATION_MASK = 0x02
            private const val DATA_LENGTH_MASK = 0x01

            /**
             * Parses the second v2.4 flag byte.
             *
             * @param raw raw flag byte.
             * @return parsed flags.
             */
            fun fromByte(raw: Byte): V24 {
                val value = raw.toInt() and 0xFF
                return V24(
                    grouping = value and GROUPING_MASK != 0,
                    compression = value and COMPRESSION_MASK != 0,
                    encryption = value and ENCRYPTION_MASK != 0,
                    unsynchronisation = value and UNSYNCHRONISATION_MASK != 0,
                    dataLengthIndicator = value and DATA_LENGTH_MASK != 0,
                )
            }
        }
    }
}
