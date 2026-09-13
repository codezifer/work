package de.carsten.android.muzzic.id3.model.frame.binary
import de.carsten.android.muzzic.id3.model.frame.BinaryDataFrame
import de.carsten.android.muzzic.id3.model.frame.FrameHeader
import de.carsten.android.muzzic.id3.model.tag.DataBytes
import de.carsten.android.muzzic.id3.model.tag.TimestampFormat
import de.carsten.android.muzzic.id3.model.visitor.Id3FrameVisitor

/**
 * Synchronised tempo codes (SYTC).
 *
 * Tempo descriptors must be chronologically ordered. At most one per tag.
 *
 * @property header frame header.
 * @property format timestamp unit.
 * @property tempoData encoded tempo and timestamp pairs.
 */
data class SytcFrame(override val header: FrameHeader, val format: TimestampFormat, val tempoData: DataBytes) : BinaryDataFrame {
    override fun <R> accept(visitor: Id3FrameVisitor<R>): R = visitor.visitSytc(this)
}
