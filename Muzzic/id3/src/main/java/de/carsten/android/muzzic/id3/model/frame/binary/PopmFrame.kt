package de.carsten.android.muzzic.id3.model.frame.binary
import de.carsten.android.muzzic.id3.model.frame.BinaryDataFrame
import de.carsten.android.muzzic.id3.model.frame.FrameHeader
import de.carsten.android.muzzic.id3.model.tag.DataBytes
import de.carsten.android.muzzic.id3.model.visitor.Id3FrameVisitor

/**
 * Popularimeter rating (POPM).
 *
 * Rating is 1-255 (1 worst, 255 best), 0 means unknown. Multiple frames
 * per tag allowed, unique by email.
 *
 * @property header frame header.
 * @property email user email address.
 * @property rating rating byte.
 * @property counter personal play counter, null if omitted.
 */
data class PopmFrame(override val header: FrameHeader, val email: String, val rating: UByte, val counter: DataBytes?) : BinaryDataFrame {
    override fun <R> accept(visitor: Id3FrameVisitor<R>): R = visitor.visitPopm(this)
}
