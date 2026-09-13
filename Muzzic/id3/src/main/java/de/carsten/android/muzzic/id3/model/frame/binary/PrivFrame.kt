package de.carsten.android.muzzic.id3.model.frame.binary
import de.carsten.android.muzzic.id3.model.frame.FrameHeader
import de.carsten.android.muzzic.id3.model.frame.OwnerIdentifierFrame
import de.carsten.android.muzzic.id3.model.tag.DataBytes
import de.carsten.android.muzzic.id3.model.visitor.Id3FrameVisitor

/**
 * Private frame for producer-specific data (PRIV).
 *
 * Multiple frames per tag allowed with different content.
 *
 * @property header frame header.
 * @property ownerIdentifier producer URL or email.
 * @property privateData producer-specific bytes.
 */
data class PrivFrame(override val header: FrameHeader, override val ownerIdentifier: String, val privateData: DataBytes) : OwnerIdentifierFrame {
    override fun <R> accept(visitor: Id3FrameVisitor<R>): R = visitor.visitPriv(this)
}
