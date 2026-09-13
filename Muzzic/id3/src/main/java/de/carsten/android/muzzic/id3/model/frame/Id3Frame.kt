package de.carsten.android.muzzic.id3.model.frame

import de.carsten.android.muzzic.id3.model.visitor.Id3FrameVisitor

/**
 * Typed ID3v2 frame with visitor support.
 *
 * Every declared frame identifier maps to exactly one implementation.
 * Unknown or experimental frames use [UnknownFrame] so parsers can skip
 * them without data loss. The generic type parameter of [accept] lets
 * each visitor choose its own result type.
 *
 * The hierarchy is intentionally open (not sealed) so frame classes can
 * live in focused sub-packages; operations dispatch through
 * [Id3FrameVisitor] instead of exhaustive `when` expressions.
 */
interface Id3Frame {
    /** Frame header with identifier, size and flags. */
    val header: FrameHeader

    /**
     * Accepts a visitor for double dispatch.
     *
     * @param visitor visitor to handle this frame.
     * @return visitor result.
     */
    fun <R> accept(visitor: Id3FrameVisitor<R>): R
}
