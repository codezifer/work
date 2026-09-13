package de.carsten.android.muzzic.id3.model.tag

import de.carsten.android.muzzic.id3.exceptions.Id3Exception

/**
 * 32 bit synchsafe integer codec.
 *
 * Synchsafe integers keep bit 7 of every byte zeroed so sizes can never
 * introduce false MPEG synchronisations. Only 28 of 32 bits carry
 * information (max 256 MB). Used for tag sizes and, in v2.4, frame sizes.
 */
object SynchsafeInt {
    /** Encoded length of a synchsafe integer in bytes. */
    const val SIZE = 4

    /** Maximum encodable value (28 bits). */
    const val MAX_VALUE = 0x0FFF_FFFF

    /**
     * Decodes four synchsafe bytes.
     *
     * High bits are masked out tolerantly instead of rejected so slightly
     * malformed tags still parse.
     *
     * @param bytes source array holding at least [SIZE] bytes from [offset].
     * @param offset start offset inside [bytes].
     * @return decoded value.
     * @throws Id3Exception if fewer than [SIZE] bytes are available.
     */
    fun decode(bytes: ByteArray, offset: Int = 0): Int {
        if (bytes.size - offset < SIZE) throw Id3Exception("Need $SIZE bytes to decode a synchsafe integer")
        return (bytes[offset].toInt() and 0x7F shl 21) or
            (bytes[offset + 1].toInt() and 0x7F shl 14) or
            (bytes[offset + 2].toInt() and 0x7F shl 7) or
            (bytes[offset + 3].toInt() and 0x7F)
    }

    /**
     * Encodes a value as four synchsafe bytes (big endian).
     *
     * @param value value in range 0..[MAX_VALUE].
     * @return four synchsafe bytes.
     * @throws Id3Exception if the value is out of range.
     */
    fun encode(value: Int): ByteArray {
        if (value < 0 || value > MAX_VALUE) throw Id3Exception("Synchsafe integer out of range: $value")
        return byteArrayOf(
            (value shr 21 and 0x7F).toByte(),
            (value shr 14 and 0x7F).toByte(),
            (value shr 7 and 0x7F).toByte(),
            (value and 0x7F).toByte(),
        )
    }
}
