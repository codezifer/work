package de.carsten.android.muzzic.id3.model.frame.binary
import de.carsten.android.muzzic.id3.model.frame.BinaryDataFrame
import de.carsten.android.muzzic.id3.model.frame.FrameHeader
import de.carsten.android.muzzic.id3.model.visitor.Id3FrameVisitor

/**
 * Audio seek point index for variable bit rate seeking (ASPI).
 *
 * Requires a TLEN frame. At most one per tag. New in v2.4.
 *
 * @property header frame header.
 * @property indexedDataStart byte offset of the indexed audio data.
 * @property indexedDataLength byte length of the indexed audio data.
 * @property fractions numerators of fractional offsets, denominator is 2^bitsPerPoint.
 * @property bitsPerPoint precision per index point, 8 or 16.
 */
data class AspiFrame(override val header: FrameHeader, val indexedDataStart: UInt, val indexedDataLength: UInt, val fractions: List<Int>, val bitsPerPoint: UByte) :
    BinaryDataFrame {
    override fun <R> accept(visitor: Id3FrameVisitor<R>): R = visitor.visitAspi(this)
}
