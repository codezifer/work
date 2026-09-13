package de.carsten.android.muzzic.id3.model.visitor

import de.carsten.android.muzzic.id3.model.frame.Id3Frame
import de.carsten.android.muzzic.id3.model.tag.TagExtendedHeader
import de.carsten.android.muzzic.id3.model.tag.TagFooter
import de.carsten.android.muzzic.id3.model.tag.TagHeader

/**
 * Visitor over a complete [de.carsten.android.muzzic.id3.model.tag.Id3Tag].
 *
 * Unlike the frame visitor, all methods are abstract: a tag has exactly
 * five structural parts, so exhaustive handling stays cheap while tag
 * operations remain decoupled from the model.
 *
 * @param R result type of the visit operation.
 */
interface Id3TagVisitor<R> {
    /**
     * Visits the tag header.
     *
     * @param header tag header.
     * @return visitor result.
     */
    fun visitHeader(header: TagHeader): R

    /**
     * Visits the extended header.
     *
     * @param extendedHeader extended header, null if absent.
     * @return visitor result.
     */
    fun visitExtendedHeader(extendedHeader: TagExtendedHeader?): R

    /**
     * Visits a single frame.
     *
     * @param frame visited frame.
     * @return visitor result.
     */
    fun visitFrame(frame: Id3Frame): R

    /**
     * Visits the tag footer.
     *
     * @param footer tag footer, null if absent.
     * @return visitor result.
     */
    fun visitFooter(footer: TagFooter?): R
}
