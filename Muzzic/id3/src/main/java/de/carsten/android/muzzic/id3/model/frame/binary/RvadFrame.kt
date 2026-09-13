package de.carsten.android.muzzic.id3.model.frame.binary
import de.carsten.android.muzzic.id3.model.frame.BinaryDataFrame
import de.carsten.android.muzzic.id3.model.frame.FrameHeader
import de.carsten.android.muzzic.id3.model.tag.DataBytes
import de.carsten.android.muzzic.id3.model.visitor.Id3FrameVisitor

/**
 * Relative volume adjustment, deprecated and replaced by RVA2 (RVAD).
 *
 * Front channels are always present; back, centre and bass channels are
 * appended as raw extension data when present.
 *
 * @property header frame header.
 * @property incrementDecrement channel direction bits (bit 0 right, 1 left, 2 right back, ...).
 * @property bitsUsedForVolume bits used per volume description, never $00.
 * @property frontRightChange front right volume change bytes.
 * @property frontLeftChange front left volume change bytes.
 * @property frontRightPeak front right peak volume bytes, null if omitted.
 * @property frontLeftPeak front left peak volume bytes, null if omitted.
 * @property extendedChannels back, centre and bass channel data, null if absent.
 */
data class RvadFrame(
    override val header: FrameHeader,
    val incrementDecrement: UByte,
    val bitsUsedForVolume: UByte,
    val frontRightChange: DataBytes,
    val frontLeftChange: DataBytes,
    val frontRightPeak: DataBytes?,
    val frontLeftPeak: DataBytes?,
    val extendedChannels: DataBytes?,
) : BinaryDataFrame {
    override fun <R> accept(visitor: Id3FrameVisitor<R>): R = visitor.visitRvad(this)
}
