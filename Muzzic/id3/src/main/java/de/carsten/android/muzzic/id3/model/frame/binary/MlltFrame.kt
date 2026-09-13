package de.carsten.android.muzzic.id3.model.frame.binary
import de.carsten.android.muzzic.id3.model.frame.BinaryDataFrame
import de.carsten.android.muzzic.id3.model.frame.FrameHeader
import de.carsten.android.muzzic.id3.model.tag.DataBytes
import de.carsten.android.muzzic.id3.model.visitor.Id3FrameVisitor

/**
 * MPEG location lookup table for seeking (MLLT).
 *
 * At most one per tag.
 *
 * @property header frame header.
 * @property mpegFramesBetweenReference frame counter increment per reference.
 * @property bytesBetweenReference nominal bytes between references.
 * @property millisBetweenReference nominal milliseconds between references.
 * @property bitsForBytesDeviation bits describing byte deviation per reference.
 * @property bitsForMillisDeviation bits describing millisecond deviation per reference.
 * @property references packed deviation references.
 */
data class MlltFrame(
    override val header: FrameHeader,
    val mpegFramesBetweenReference: Int,
    val bytesBetweenReference: Int,
    val millisBetweenReference: Int,
    val bitsForBytesDeviation: UByte,
    val bitsForMillisDeviation: UByte,
    val references: DataBytes,
) : BinaryDataFrame {
    override fun <R> accept(visitor: Id3FrameVisitor<R>): R = visitor.visitMllt(this)
}
