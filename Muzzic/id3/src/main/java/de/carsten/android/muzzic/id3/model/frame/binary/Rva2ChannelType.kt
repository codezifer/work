package de.carsten.android.muzzic.id3.model.frame.binary

/**
 * Relative volume adjustment channel types (RVA2).
 *
 * @property code raw channel byte.
 */
enum class Rva2ChannelType(val code: Byte) {

    /** Other channel. */
    OTHER(code = 0x00),

    /** Master volume. */
    MASTER_VOLUME(code = 0x01),

    /** Front right. */
    FRONT_RIGHT(code = 0x02),

    /** Front left. */
    FRONT_LEFT(code = 0x03),

    /** Back right. */
    BACK_RIGHT(code = 0x04),

    /** Back left. */
    BACK_LEFT(code = 0x05),

    /** Front centre. */
    FRONT_CENTRE(code = 0x06),

    /** Back centre. */
    BACK_CENTRE(code = 0x07),

    /** Subwoofer. */
    SUBWOOFER(code = 0x08),
    ;

    companion object {
        /**
         * Resolves a channel type from its raw byte.
         *
         * @param code raw channel byte.
         * @return matching type, defaults to [OTHER].
         */
        fun fromCode(code: Byte): Rva2ChannelType = entries.firstOrNull { it.code == code } ?: OTHER
    }
}
