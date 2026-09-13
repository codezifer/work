package de.carsten.android.muzzic.id3.model.frame.binary

import de.carsten.android.muzzic.id3.model.frame.BinaryDataFrame
import de.carsten.android.muzzic.id3.model.frame.FrameHeader
import de.carsten.android.muzzic.id3.model.visitor.Id3FrameVisitor

/**
 * Relative volume adjustment (RVA2).
 *
 * Multiple frames per tag allowed, unique by identification.
 *
 * @property header frame header.
 * @property identification situation or device this adjustment applies to.
 * @property channels per-channel adjustments.
 */
data class Rva2Frame(override val header: FrameHeader, val identification: String, val channels: List<Rva2ChannelAdjust>) : BinaryDataFrame {
    override fun <R> accept(visitor: Id3FrameVisitor<R>): R = visitor.visitRva2(this)
}
