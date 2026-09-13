package de.carsten.android.muzzic.id3.model.frame.binary
import de.carsten.android.muzzic.id3.model.frame.BinaryDataFrame
import de.carsten.android.muzzic.id3.model.frame.FrameHeader
import de.carsten.android.muzzic.id3.model.tag.DataBytes
import de.carsten.android.muzzic.id3.model.tag.TimestampFormat
import de.carsten.android.muzzic.id3.model.visitor.Id3FrameVisitor

/**
 * Position synchronisation for stream pickup (POSS). At most one per tag.
 *
 * @property header frame header.
 * @property format timestamp unit.
 * @property position stream offset of the next frame.
 */
data class PossFrame(override val header: FrameHeader, val format: TimestampFormat, val position: DataBytes) : BinaryDataFrame {
    override fun <R> accept(visitor: Id3FrameVisitor<R>): R = visitor.visitPoss(this)
}
