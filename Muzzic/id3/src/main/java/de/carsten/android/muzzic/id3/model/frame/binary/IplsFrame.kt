package de.carsten.android.muzzic.id3.model.frame.binary

import de.carsten.android.muzzic.id3.model.frame.BinaryDataFrame
import de.carsten.android.muzzic.id3.model.frame.FrameHeader
import de.carsten.android.muzzic.id3.model.tag.TextEncoding
import de.carsten.android.muzzic.id3.model.visitor.Id3FrameVisitor

/**
 * Involved people list, deprecated and replaced by TMCL and TIPL (IPLS).
 *
 * At most one per tag.
 *
 * @property header frame header.
 * @property encoding text encoding.
 * @property entries involvement/name pairs.
 */
data class IplsFrame(override val header: FrameHeader, val encoding: TextEncoding, val entries: List<InvolvedPerson>) : BinaryDataFrame {
    override fun <R> accept(visitor: Id3FrameVisitor<R>): R = visitor.visitIpls(this)
}
