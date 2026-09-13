package de.carsten.android.muzzic.id3.model.frame.binary
import de.carsten.android.muzzic.id3.model.frame.BinaryDataFrame
import de.carsten.android.muzzic.id3.model.frame.FrameHeader
import de.carsten.android.muzzic.id3.model.tag.DataBytes
import de.carsten.android.muzzic.id3.model.tag.TextEncoding
import de.carsten.android.muzzic.id3.model.visitor.Id3FrameVisitor

/**
 * General encapsulated object for arbitrary files (GEOB).
 *
 * Multiple frames per tag allowed, unique by descriptor.
 *
 * @property header frame header.
 * @property encoding text encoding of filename and description.
 * @property mimeType MIME type, always ISO-8859-1.
 * @property filename case sensitive filename.
 * @property description content description.
 * @property data encapsulated object bytes.
 */
data class GeobFrame(override val header: FrameHeader, val encoding: TextEncoding, val mimeType: String, val filename: String, val description: String, val data: DataBytes) :
    BinaryDataFrame {
    override fun <R> accept(visitor: Id3FrameVisitor<R>): R = visitor.visitGeob(this)
}
