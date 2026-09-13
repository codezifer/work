package de.carsten.android.muzzic.id3.model.frame.binary
import de.carsten.android.muzzic.id3.model.frame.FrameHeader
import de.carsten.android.muzzic.id3.model.frame.OwnerIdentifierFrame
import de.carsten.android.muzzic.id3.model.tag.DataBytes
import de.carsten.android.muzzic.id3.model.visitor.Id3FrameVisitor

/**
 * Audio encryption descriptor (AENC).
 *
 * Multiple frames per tag allowed, unique by owner.
 *
 * @property header frame header.
 * @property ownerIdentifier encryption owner URL or email.
 * @property previewStart unencrypted preview start in frames.
 * @property previewLength unencrypted preview length in frames.
 * @property encryptionInfo decryption data block.
 */
data class AencFrame(override val header: FrameHeader, override val ownerIdentifier: String, val previewStart: Int, val previewLength: Int, val encryptionInfo: DataBytes) :
    OwnerIdentifierFrame {
    override fun <R> accept(visitor: Id3FrameVisitor<R>): R = visitor.visitAenc(this)
}
