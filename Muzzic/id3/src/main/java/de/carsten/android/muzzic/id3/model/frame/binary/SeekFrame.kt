package de.carsten.android.muzzic.id3.model.frame.binary
import de.carsten.android.muzzic.id3.model.frame.BinaryDataFrame
import de.carsten.android.muzzic.id3.model.frame.FrameHeader
import de.carsten.android.muzzic.id3.model.visitor.Id3FrameVisitor

/**
 * Seek frame locating other tags in a file or stream (SEEK).
 *
 * At most one per tag. New in v2.4.
 *
 * @property header frame header.
 * @property minOffsetToNextTag minimum offset from this tag's end to the next tag.
 */
data class SeekFrame(override val header: FrameHeader, val minOffsetToNextTag: Int) : BinaryDataFrame {
    override fun <R> accept(visitor: Id3FrameVisitor<R>): R = visitor.visitSeek(this)
}
