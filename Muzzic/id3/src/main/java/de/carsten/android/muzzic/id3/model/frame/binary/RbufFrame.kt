package de.carsten.android.muzzic.id3.model.frame.binary
import de.carsten.android.muzzic.id3.model.frame.BinaryDataFrame
import de.carsten.android.muzzic.id3.model.frame.FrameHeader
import de.carsten.android.muzzic.id3.model.visitor.Id3FrameVisitor

/**
 * Recommended buffer size for streaming (RBUF). At most one per tag.
 *
 * @property header frame header.
 * @property bufferSize recommended buffer size in bytes.
 * @property embeddedInfo maximum-size tags may occur inside the audio stream.
 * @property offsetToNextTag bytes from this tag's end to the next tag header, null if omitted.
 */
data class RbufFrame(override val header: FrameHeader, val bufferSize: Int, val embeddedInfo: Boolean, val offsetToNextTag: UInt?) : BinaryDataFrame {
    override fun <R> accept(visitor: Id3FrameVisitor<R>): R = visitor.visitRbuf(this)
}
