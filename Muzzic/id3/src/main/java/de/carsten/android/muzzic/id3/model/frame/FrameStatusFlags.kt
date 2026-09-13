package de.carsten.android.muzzic.id3.model.frame

import de.carsten.android.muzzic.id3.model.tag.Id3Version

/**
 * Status flags shared by both major versions (first flag byte).
 *
 * Unknown set bits in this byte forbid altering the frame; unknown bits
 * in the format byte likely render the frame unreadable.
 *
 * @property tagAlterDiscard discard frame if unknown and the tag is altered.
 * @property fileAlterDiscard discard frame if unknown and the audio (excluding tag) is altered.
 * @property readOnly frame content is intended to be read only.
 */
data class FrameStatusFlags(val tagAlterDiscard: Boolean = false, val fileAlterDiscard: Boolean = false, val readOnly: Boolean = false) {
    companion object {
        /**
         * Parses the first flag byte.
         *
         * v2.3 uses `%abc00000` (bits 7-5), v2.4 uses `%0abc0000`
         * (bits 6-4).
         *
         * @param version frame version.
         * @param raw raw flag byte.
         * @return parsed flags.
         */
        fun fromByte(version: Id3Version, raw: Byte): FrameStatusFlags {
            val value = raw.toInt() and 0xFF
            val shift = if (version == Id3Version.V2_3) 5 else 4
            return FrameStatusFlags(
                tagAlterDiscard = value shr (shift + 2) and 0x01 != 0,
                fileAlterDiscard = value shr (shift + 1) and 0x01 != 0,
                readOnly = value shr shift and 0x01 != 0,
            )
        }
    }
}
