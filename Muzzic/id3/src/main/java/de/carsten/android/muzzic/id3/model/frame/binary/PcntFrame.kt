package de.carsten.android.muzzic.id3.model.frame.binary
import de.carsten.android.muzzic.id3.model.frame.BinaryDataFrame
import de.carsten.android.muzzic.id3.model.frame.FrameHeader
import de.carsten.android.muzzic.id3.model.tag.DataBytes
import de.carsten.android.muzzic.id3.model.visitor.Id3FrameVisitor

/**
 * Play counter (PCNT). At most one per tag.
 *
 * The counter grows by one byte in front when reaching all one's.
 * At least 32 bits long initially.
 *
 * @property header frame header.
 * @property counter big-endian counter bytes.
 */
data class PcntFrame(override val header: FrameHeader, val counter: DataBytes) : BinaryDataFrame {
    override fun <R> accept(visitor: Id3FrameVisitor<R>): R = visitor.visitPcnt(this)
}
