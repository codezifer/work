package de.carsten.android.muzzic.id3.model.frame.binary

import de.carsten.android.muzzic.id3.model.frame.BinaryDataFrame
import de.carsten.android.muzzic.id3.model.frame.FrameHeader
import de.carsten.android.muzzic.id3.model.tag.DataBytes
import de.carsten.android.muzzic.id3.model.tag.TextEncoding
import de.carsten.android.muzzic.id3.model.visitor.Id3FrameVisitor

/**
 * Attached picture (APIC).
 *
 * Use "-->" as MIME type with a URL as picture data for linked images.
 * Multiple frames per tag allowed, unique by description.
 *
 * @property header frame header.
 * @property encoding text encoding of the description.
 * @property mimeType MIME type and subtype, "image/" is implied if omitted.
 * @property pictureType picture kind.
 * @property description short picture description.
 * @property pictureData picture bytes or linked URL.
 */
data class ApicFrame(
    override val header: FrameHeader,
    val encoding: TextEncoding,
    val mimeType: String,
    val pictureType: PictureType,
    val description: String,
    val pictureData: DataBytes,
) : BinaryDataFrame {
    override fun <R> accept(visitor: Id3FrameVisitor<R>): R = visitor.visitApic(this)
}
