package de.carsten.android.muzzic.id3.model.frame.binary
import de.carsten.android.muzzic.id3.model.frame.BinaryDataFrame
import de.carsten.android.muzzic.id3.model.frame.FrameHeader
import de.carsten.android.muzzic.id3.model.visitor.Id3FrameVisitor

/**
 * Reverb settings (RVRB). At most one per tag.
 *
 * Delays are milliseconds, bounces count echoes ($FF is infinite),
 * feedback and premix range from $00 (0 %) to $FF (100 %).
 *
 * @property header frame header.
 * @property reverbLeftMs delay between left bounces in ms.
 * @property reverbRightMs delay between right bounces in ms.
 * @property bouncesLeft number of left bounces.
 * @property bouncesRight number of right bounces.
 * @property feedbackLeftToLeft left to left feedback.
 * @property feedbackLeftToRight left to right feedback.
 * @property feedbackRightToRight right to right feedback.
 * @property feedbackRightToLeft right to left feedback.
 * @property premixLeftToRight left sound mixed into right before reverb.
 * @property premixRightToLeft right sound mixed into left before reverb.
 */
data class RvrbFrame(
    override val header: FrameHeader,
    val reverbLeftMs: Int,
    val reverbRightMs: Int,
    val bouncesLeft: UByte,
    val bouncesRight: UByte,
    val feedbackLeftToLeft: UByte,
    val feedbackLeftToRight: UByte,
    val feedbackRightToRight: UByte,
    val feedbackRightToLeft: UByte,
    val premixLeftToRight: UByte,
    val premixRightToLeft: UByte,
) : BinaryDataFrame {
    override fun <R> accept(visitor: Id3FrameVisitor<R>): R = visitor.visitRvrb(this)
}
