package de.carsten.android.muzzic.id3.model.tag

/**
 * 10 byte tag header opening an ID3v2 tag.
 *
 * Layout: `"ID3" + version (2 bytes) + flags (1 byte) + size (4 bytes synchsafe)`.
 * The size covers extended header, frames and padding, excluding header
 * and footer.
 *
 * @property version tag version.
 * @property flags header flags.
 * @property tagSize tag size in bytes, excluding header and footer.
 */
data class TagHeader(val version: Id3Version, val flags: TagHeaderFlags = TagHeaderFlags(), val tagSize: Int = 0) {
    companion object {
        /** Fixed header length in bytes. */
        const val SIZE = 10

        /** File identifier marking a tag header. */
        const val IDENTIFIER = "ID3"
    }
}
