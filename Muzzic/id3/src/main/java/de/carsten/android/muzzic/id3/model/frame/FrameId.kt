package de.carsten.android.muzzic.id3.model.frame

import de.carsten.android.muzzic.id3.exceptions.Id3Exception

/**
 * Frame identifier (A-Z, 0-9).
 *
 * Identifiers are four characters; the v2.3 LINK frame is the single
 * documented exception with three characters. Identifiers starting with
 * X, Y or Z are experimental and free to use. All other identifiers are
 * either used by the specification or reserved.
 *
 * @property value identifier, e.g. "TIT2".
 */
@JvmInline
value class FrameId(val value: String) {
    init {
        require((value.length == LENGTH || value.length == LENGTH - 1) && value.all { it in 'A'..'Z' || it in '0'..'9' }) {
            "FrameId must be $LENGTH chars A-Z/0-9: $value"
        }
    }

    /**
     * Human-readable identifier.
     *
     * @return the four-character identifier.
     */
    override fun toString(): String = value

    companion object {
        /** Fixed identifier length. */
        const val LENGTH = 4

        /**
         * Parses four raw bytes into a [FrameId].
         *
         * @param bytes source array with at least [LENGTH] bytes from [offset].
         * @param offset start offset.
         * @return parsed identifier.
         * @throws Id3Exception if the bytes are not a valid identifier.
         */
        fun parse(bytes: ByteArray, offset: Int = 0): FrameId {
            if (bytes.size - offset < LENGTH) throw Id3Exception("Need $LENGTH bytes for a frame id")
            val value = bytes.decodeToString(offset, offset + LENGTH)
            if (value.all { it == '\u0000' }) throw Id3Exception("Reached padding, no further frames")
            return FrameId(value)
        }
    }
}
