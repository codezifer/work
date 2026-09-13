package de.carsten.android.muzzic.id3.model.frame.language
import de.carsten.android.muzzic.id3.model.frame.FrameHeader
import de.carsten.android.muzzic.id3.model.frame.LanguageDescriptorFrame
import de.carsten.android.muzzic.id3.model.tag.LanguageCode
import de.carsten.android.muzzic.id3.model.tag.TextEncoding
import de.carsten.android.muzzic.id3.model.visitor.Id3FrameVisitor

/**
 * Unsynchronised lyrics or text transcription (USLT).
 *
 * Newlines allowed. Unique per language and descriptor.
 *
 * @property header frame header.
 * @property encoding text encoding.
 * @property language content language.
 * @property descriptor content descriptor.
 * @property lyrics actual lyrics or transcription.
 */
data class UsltFrame(
    override val header: FrameHeader,
    override val encoding: TextEncoding,
    override val language: LanguageCode,
    override val descriptor: String,
    val lyrics: String,
) : LanguageDescriptorFrame {
    override fun <R> accept(visitor: Id3FrameVisitor<R>): R = visitor.visitUslt(this)
}
