package de.carsten.android.muzzic.id3.model.frame

import de.carsten.android.muzzic.id3.model.tag.DataBytes
import de.carsten.android.muzzic.id3.model.visitor.Id3FrameVisitor

/**
 * Fallback for unknown, experimental (X/Y/Z) or undecodable frames.
 *
 * Parsers must preserve these frames when the status flags require it
 * instead of dropping them.
 *
 * @property header frame header.
 * @property raw undecoded frame body.
 */
data class UnknownFrame(override val header: FrameHeader, val raw: DataBytes) : Id3Frame {
    override fun <R> accept(visitor: Id3FrameVisitor<R>): R = visitor.visitUnknown(this)
}
