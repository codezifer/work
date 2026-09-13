package de.carsten.android.muzzic.id3.model.frame.binary

import de.carsten.android.muzzic.id3.model.frame.BinaryDataFrame
import de.carsten.android.muzzic.id3.model.frame.FrameHeader
import de.carsten.android.muzzic.id3.model.visitor.Id3FrameVisitor

/**
 * Equalization, deprecated and replaced by EQU2 (EQUA).
 *
 * Bands should be ordered by frequency. At most one per tag.
 *
 * @property header frame header.
 * @property adjustmentBits bits used per adjustment value, never $00.
 * @property bands equalisation bands.
 */
data class EquaFrame(override val header: FrameHeader, val adjustmentBits: UByte, val bands: List<EquaBand>) : BinaryDataFrame {
    override fun <R> accept(visitor: Id3FrameVisitor<R>): R = visitor.visitEqua(this)
}
