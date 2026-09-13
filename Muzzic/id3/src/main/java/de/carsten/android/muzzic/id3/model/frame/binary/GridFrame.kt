package de.carsten.android.muzzic.id3.model.frame.binary
import de.carsten.android.muzzic.id3.model.frame.FrameHeader
import de.carsten.android.muzzic.id3.model.frame.OwnerIdentifierFrame
import de.carsten.android.muzzic.id3.model.tag.DataBytes
import de.carsten.android.muzzic.id3.model.visitor.Id3FrameVisitor

/**
 * Group identification registration (GRID).
 *
 * The group symbol ($80-F0 in v2.4) links grouped frames to this
 * registration. Unique by symbol and by owner.
 *
 * @property header frame header.
 * @property ownerIdentifier grouping owner URL or email.
 * @property groupSymbol symbol referenced by grouped frames.
 * @property groupData group-specific data, e.g. a signature.
 */
data class GridFrame(override val header: FrameHeader, override val ownerIdentifier: String, val groupSymbol: UByte, val groupData: DataBytes) : OwnerIdentifierFrame {
    override fun <R> accept(visitor: Id3FrameVisitor<R>): R = visitor.visitGrid(this)
}
