package de.carsten.android.muzzic.id3.model.frame.url
import de.carsten.android.muzzic.id3.model.frame.FrameHeader
import de.carsten.android.muzzic.id3.model.frame.Id3Frame
import de.carsten.android.muzzic.id3.model.tag.TextEncoding
import de.carsten.android.muzzic.id3.model.visitor.Id3FrameVisitor

/**
 * User defined URL link frame (WXXX).
 *
 * The URL itself is always ISO-8859-1 encoded. Multiple frames per tag
 * allowed, unique by description.
 *
 * @property header frame header.
 * @property encoding text encoding of the description.
 * @property description description of the URL.
 * @property url actual URL.
 */
data class WxxxFrame(override val header: FrameHeader, val encoding: TextEncoding, val description: String, val url: String) : Id3Frame {
    override fun <R> accept(visitor: Id3FrameVisitor<R>): R = visitor.visitWxxx(this)
}
