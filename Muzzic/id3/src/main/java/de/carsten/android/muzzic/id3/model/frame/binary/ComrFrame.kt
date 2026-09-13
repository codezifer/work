package de.carsten.android.muzzic.id3.model.frame.binary

import de.carsten.android.muzzic.id3.model.frame.BinaryDataFrame
import de.carsten.android.muzzic.id3.model.frame.FrameHeader
import de.carsten.android.muzzic.id3.model.tag.DataBytes
import de.carsten.android.muzzic.id3.model.tag.TextEncoding
import de.carsten.android.muzzic.id3.model.visitor.Id3FrameVisitor

/**
 * Commercial frame bundling competing offers (COMR).
 *
 * Multiple frames per tag allowed, but no two may be identical.
 *
 * @property header frame header.
 * @property encoding text encoding of seller and description.
 * @property priceString slash-separated prices with ISO-4217 prefixes.
 * @property validUntil price validity as YYYYMMDD.
 * @property contactUrl seller contact URL.
 * @property receivedAs delivery method.
 * @property sellerName seller name.
 * @property description short product description.
 * @property mimeType seller logo MIME type, null if no logo attached.
 * @property sellerLogo seller logo bytes, null if no logo attached.
 */
data class ComrFrame(
    override val header: FrameHeader,
    val encoding: TextEncoding,
    val priceString: String,
    val validUntil: String,
    val contactUrl: String,
    val receivedAs: ReceivedAs,
    val sellerName: String,
    val description: String,
    val mimeType: String?,
    val sellerLogo: DataBytes?,
) : BinaryDataFrame {
    override fun <R> accept(visitor: Id3FrameVisitor<R>): R = visitor.visitComr(this)
}
