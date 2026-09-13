package de.carsten.android.muzzic.id3.model.frame.binary
import de.carsten.android.muzzic.id3.model.frame.FrameHeader
import de.carsten.android.muzzic.id3.model.frame.OwnerIdentifierFrame
import de.carsten.android.muzzic.id3.model.tag.DataBytes
import de.carsten.android.muzzic.id3.model.visitor.Id3FrameVisitor

/**
 * Unique file identifier for database lookups (UFID).
 *
 * Multiple frames per tag allowed, unique by owner.
 *
 * @property header frame header.
 * @property ownerIdentifier database owner URL or email.
 * @property identifier database identifier, up to 64 bytes.
 */
data class UfidFrame(override val header: FrameHeader, override val ownerIdentifier: String, val identifier: DataBytes) : OwnerIdentifierFrame {
    override fun <R> accept(visitor: Id3FrameVisitor<R>): R = visitor.visitUfid(this)
}
