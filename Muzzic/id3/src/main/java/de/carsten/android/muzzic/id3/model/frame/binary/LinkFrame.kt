package de.carsten.android.muzzic.id3.model.frame.binary
import de.carsten.android.muzzic.id3.model.frame.BinaryDataFrame
import de.carsten.android.muzzic.id3.model.frame.FrameHeader
import de.carsten.android.muzzic.id3.model.frame.FrameId
import de.carsten.android.muzzic.id3.model.visitor.Id3FrameVisitor

/**
 * Linked information from another tag or file (LINK).
 *
 * A linked frame counts as part of the tag with the same uniqueness
 * restrictions as a physical frame.
 *
 * @property header frame header.
 * @property linkedFrameId identifier of the linked frame.
 * @property url reference to the file holding the linked frame.
 * @property additionalData frame-specific id data, e.g. descriptor or language.
 */
data class LinkFrame(override val header: FrameHeader, val linkedFrameId: FrameId, val url: String, val additionalData: String) : BinaryDataFrame {
    override fun <R> accept(visitor: Id3FrameVisitor<R>): R = visitor.visitLink(this)
}
