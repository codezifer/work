package de.carsten.android.muzzic.id3.model.frame

/**
 * 10 byte frame header preceding every frame body.
 *
 * v2.3 stores the size as a plain 32 bit integer, v2.4 as a 32 bit
 * synchsafe integer. In both versions the size excludes the header
 * itself and covers body plus any appended format-flag fields.
 */
sealed interface FrameHeader {
    /** Frame identifier. */
    val frameId: FrameId

    /** Frame size in bytes, excluding the 10 byte header. */
    val size: Int

    /** Status flags (first flag byte). */
    val status: FrameStatusFlags

    /**
     * v2.3 frame header.
     *
     * Extra fields follow the header in flag order (decompressed size,
     * encryption method, group identifier).
     *
     * @property frameId frame identifier.
     * @property size frame size as plain 32 bit integer, excluding the header.
     * @property status status flags.
     * @property format v2.3 format flags.
     * @property decompressedSize decompressed body size, present if compressed.
     * @property encryptionMethod encryption method symbol, present if encrypted.
     * @property groupId group identifier, present if grouped.
     */
    data class V23(
        override val frameId: FrameId,
        override val size: Int,
        override val status: FrameStatusFlags = FrameStatusFlags(),
        val format: FrameFormatFlags.V23 = FrameFormatFlags.V23(),
        val decompressedSize: Int? = null,
        val encryptionMethod: UByte? = null,
        val groupId: UByte? = null,
    ) : FrameHeader

    /**
     * v2.4 frame header.
     *
     * Extra fields follow the header in flag order (group identifier,
     * data length indicator, encryption method).
     *
     * @property frameId frame identifier.
     * @property size frame size as synchsafe integer, excluding the header.
     * @property status status flags.
     * @property format v2.4 format flags.
     * @property groupId group identifier, present if grouped.
     * @property dataLengthIndicator size the frame length would have with cleared format flags.
     * @property encryptionMethod encryption method symbol, present if encrypted.
     */
    data class V24(
        override val frameId: FrameId,
        override val size: Int,
        override val status: FrameStatusFlags = FrameStatusFlags(),
        val format: FrameFormatFlags.V24 = FrameFormatFlags.V24(),
        val groupId: UByte? = null,
        val dataLengthIndicator: Int? = null,
        val encryptionMethod: UByte? = null,
    ) : FrameHeader

    companion object {
        /** Fixed frame header length in bytes. */
        const val SIZE = 10
    }
}
