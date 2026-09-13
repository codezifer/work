package de.carsten.android.muzzic.id3.model.frame.text
import de.carsten.android.muzzic.id3.model.frame.FrameHeader
import de.carsten.android.muzzic.id3.model.frame.Id3Frame
import de.carsten.android.muzzic.id3.model.tag.TextEncoding
import de.carsten.android.muzzic.id3.model.visitor.Id3FrameVisitor

/**
 * User defined text information frame (TXXX).
 *
 * Multiple frames per tag allowed, unique by description.
 *
 * @property header frame header.
 * @property encoding text encoding.
 * @property description description of the value.
 * @property value actual string value.
 */
data class TxxxFrame(override val header: FrameHeader, val encoding: TextEncoding, val description: String, val value: String) : Id3Frame {
    override fun <R> accept(visitor: Id3FrameVisitor<R>): R = visitor.visitTxxx(this)
}
