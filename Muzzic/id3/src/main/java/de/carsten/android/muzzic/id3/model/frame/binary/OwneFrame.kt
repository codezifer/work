package de.carsten.android.muzzic.id3.model.frame.binary
import de.carsten.android.muzzic.id3.model.frame.BinaryDataFrame
import de.carsten.android.muzzic.id3.model.frame.FrameHeader
import de.carsten.android.muzzic.id3.model.tag.TextEncoding
import de.carsten.android.muzzic.id3.model.visitor.Id3FrameVisitor

/**
 * Ownership frame as transaction reminder or proof (OWNE).
 *
 * At most one per tag. Pairs well with USER and TOWN.
 *
 * @property header frame header.
 * @property encoding text encoding of the seller name.
 * @property pricePaid price with ISO-4217 currency prefix, e.g. "EUR12.50".
 * @property purchaseDate purchase date as YYYYMMDD.
 * @property seller seller name.
 */
data class OwneFrame(override val header: FrameHeader, val encoding: TextEncoding, val pricePaid: String, val purchaseDate: String, val seller: String) : BinaryDataFrame {
    override fun <R> accept(visitor: Id3FrameVisitor<R>): R = visitor.visitOwne(this)
}
