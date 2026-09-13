package de.carsten.android.muzzic.id3.model.tag

import de.carsten.android.muzzic.id3.model.frame.Id3Frame

/**
 * Complete ID3v2 tag: header, optional extended header, frames,
 * optional padding and optional footer.
 *
 * Padding and footer are mutually exclusive per specification: a tag
 * with a footer must not carry padding.
 *
 * @property version tag version.
 * @property header tag header.
 * @property extendedHeader extended header, null if absent.
 * @property frames frames in tag order.
 * @property paddingSize padding bytes ($00) after the last frame.
 * @property footer tag footer, null if absent. Only v2.4.
 */
data class Id3Tag(
    val version: Id3Version,
    val header: TagHeader,
    val extendedHeader: TagExtendedHeader? = null,
    val frames: List<Id3Frame> = emptyList(),
    val paddingSize: Int = 0,
    val footer: TagFooter? = null,
)
