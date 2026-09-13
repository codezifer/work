package de.carsten.android.muzzic.id3.model.frame.binary
import de.carsten.android.muzzic.id3.model.frame.FrameHeader
import de.carsten.android.muzzic.id3.model.frame.OwnerIdentifierFrame
import de.carsten.android.muzzic.id3.model.tag.DataBytes
import de.carsten.android.muzzic.id3.model.visitor.Id3FrameVisitor

/**
 * Encryption method registration (ENCR).
 *
 * The method symbol ($80-F0 in v2.4) links encrypted frames to this
 * registration. Unique by symbol and by owner.
 *
 * @property header frame header.
 * @property ownerIdentifier encryption owner URL or email.
 * @property methodSymbol symbol referenced by encrypted frames.
 * @property encryptionData method-specific data.
 */
data class EncrFrame(override val header: FrameHeader, override val ownerIdentifier: String, val methodSymbol: UByte, val encryptionData: DataBytes) : OwnerIdentifierFrame {
    override fun <R> accept(visitor: Id3FrameVisitor<R>): R = visitor.visitEncr(this)
}
