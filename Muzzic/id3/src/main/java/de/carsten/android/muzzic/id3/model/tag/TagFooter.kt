package de.carsten.android.muzzic.id3.model.tag

/**
 * 10 byte tag footer speeding up reverse tag searches. Only v2.4.
 *
 * The footer is a copy of the header with the identifier "3DI".
 *
 * @property version tag version.
 * @property flags header flags, copied from the header.
 * @property tagSize tag size in bytes, copied from the header.
 */
data class TagFooter(val version: Id3Version, val flags: TagHeaderFlags = TagHeaderFlags(), val tagSize: Int = 0) {
    companion object {
        /** Fixed footer length in bytes. */
        const val SIZE = 10

        /** Identifier marking a tag footer. */
        const val IDENTIFIER = "3DI"
    }
}
