package de.carsten.android.muzzic.id3.model.frame.language
import de.carsten.android.muzzic.id3.model.frame.FrameHeader
import de.carsten.android.muzzic.id3.model.frame.Id3Frame
import de.carsten.android.muzzic.id3.model.tag.LanguageCode
import de.carsten.android.muzzic.id3.model.tag.TextEncoding
import de.carsten.android.muzzic.id3.model.visitor.Id3FrameVisitor

/**
 * Terms of use and ownership description (USER).
 *
 * Newlines allowed in the text.
 *
 * @property header frame header.
 * @property encoding text encoding.
 * @property language content language.
 * @property text actual terms text.
 */
data class UserFrame(override val header: FrameHeader, val encoding: TextEncoding, val language: LanguageCode, val text: String) : Id3Frame {
    override fun <R> accept(visitor: Id3FrameVisitor<R>): R = visitor.visitUser(this)
}
