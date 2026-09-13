package de.carsten.android.muzzic.id3.model.tag

/**
 * Immutable binary payload wrapper with content-based equality.
 *
 * Plain [ByteArray] only offers referential equality, which breaks
 * data class semantics for binary frames. Use this wherever the
 * specification declares `<binary data>`. A defensive copy is expected
 * from callers handing over reusable buffers.
 *
 * Equality and hash code intentionally cover the payload bytes only:
 * identical payloads at different stream positions compare equal.
 * Use [offset] and [length] explicitly when position matters.
 *
 * @property bytes raw payload bytes.
 * @property offset absolute start position of [bytes] in the input stream,
 * `0` means unknown or untracked.
 * @property length byte length in the stream, normally `bytes.size`.
 */
class DataBytes(val bytes: ByteArray, val offset: Int = 0, val length: Int = bytes.size) {
    init {
        require(length >= 0) { "DataBytes length must not be negative: $length" }
    }

    /**
     * Content-based equality.
     *
     * @param other other instance.
     * @return true if the wrapped bytes are equal.
     */
    override fun equals(other: Any?): Boolean = other is DataBytes && bytes.contentEquals(other.bytes)

    /**
     * Content-based hash code.
     *
     * @return hash code of the wrapped bytes.
     */
    override fun hashCode(): Int = bytes.contentHashCode()

    /**
     * Size-hinting string representation that never dumps payload data.
     *
     * @return short description including position and byte count.
     */
    override fun toString(): String = "DataBytes(offset=$offset, length=$length)"

    companion object {
        /**
         * Empty payload constant.
         */
        val EMPTY = DataBytes(byteArrayOf())
    }
}
