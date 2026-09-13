package de.carsten.android.muzzic.id3.model.frame.binary

/**
 * Equalisation interpolation methods (EQU2).
 *
 * @property code raw method byte.
 */
enum class Equ2Interpolation(val code: Byte) {

    /** Band: no interpolation, jump midway between points. */
    BAND(code = 0x00),

    /** Linear interpolation between adjustment points. */
    LINEAR(code = 0x01),
    ;

    companion object {
        /**
         * Resolves a method from its raw byte.
         *
         * @param code raw method byte.
         * @return matching method, defaults to [BAND].
         */
        fun fromCode(code: Byte): Equ2Interpolation = entries.firstOrNull { it.code == code } ?: BAND
    }
}
