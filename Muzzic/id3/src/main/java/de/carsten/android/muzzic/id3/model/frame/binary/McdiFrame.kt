package de.carsten.android.muzzic.id3.model.frame.binary
import de.carsten.android.muzzic.id3.model.frame.BinaryDataFrame
import de.carsten.android.muzzic.id3.model.frame.FrameHeader
import de.carsten.android.muzzic.id3.model.tag.DataBytes
import de.carsten.android.muzzic.id3.model.visitor.Id3FrameVisitor

/**
 * Music CD identifier: binary TOC dump (MCDI).
 *
 * Requires a valid TRCK frame. At most one per tag.
 *
 * @property header frame header.
 * @property toc table of contents dump, up to 804 bytes.
 */
data class McdiFrame(override val header: FrameHeader, val toc: DataBytes) : BinaryDataFrame {
    override fun <R> accept(visitor: Id3FrameVisitor<R>): R = visitor.visitMcdi(this)
}
