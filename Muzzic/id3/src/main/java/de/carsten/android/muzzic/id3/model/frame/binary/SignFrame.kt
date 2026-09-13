package de.carsten.android.muzzic.id3.model.frame.binary
import de.carsten.android.muzzic.id3.model.frame.BinaryDataFrame
import de.carsten.android.muzzic.id3.model.frame.FrameHeader
import de.carsten.android.muzzic.id3.model.tag.DataBytes
import de.carsten.android.muzzic.id3.model.visitor.Id3FrameVisitor

/**
 * Signature of a GRID-registered frame group (SIGN).
 *
 * Multiple frames per tag allowed, but no two may be identical.
 *
 * @property header frame header.
 * @property groupSymbol signed group symbol.
 * @property signature signature bytes.
 */
data class SignFrame(override val header: FrameHeader, val groupSymbol: UByte, val signature: DataBytes) : BinaryDataFrame {
    override fun <R> accept(visitor: Id3FrameVisitor<R>): R = visitor.visitSign(this)
}
