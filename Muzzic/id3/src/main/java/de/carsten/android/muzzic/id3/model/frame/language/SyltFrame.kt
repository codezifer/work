package de.carsten.android.muzzic.id3.model.frame.language

import de.carsten.android.muzzic.id3.model.frame.FrameHeader
import de.carsten.android.muzzic.id3.model.frame.LanguageDescriptorFrame
import de.carsten.android.muzzic.id3.model.tag.LanguageCode
import de.carsten.android.muzzic.id3.model.tag.TextEncoding
import de.carsten.android.muzzic.id3.model.tag.TimestampFormat
import de.carsten.android.muzzic.id3.model.visitor.Id3FrameVisitor

/**
 * Synchronised lyrics or text (SYLT).
 *
 * Each entry carries the timestamp where it belongs in the audio.
 * Unique per language and descriptor.
 *
 * @property header frame header.
 * @property encoding text encoding.
 * @property language content language.
 * @property format timestamp unit.
 * @property contentType kind of synchronised content.
 * @property descriptor content descriptor.
 * @property entries chronologically ordered sync entries.
 */
data class SyltFrame(
    override val header: FrameHeader,
    override val encoding: TextEncoding,
    override val language: LanguageCode,
    override val descriptor: String,
    val format: TimestampFormat,
    val contentType: SyltContentType,
    val entries: List<SyncEntry>,
) : LanguageDescriptorFrame {
    override fun <R> accept(visitor: Id3FrameVisitor<R>): R = visitor.visitSylt(this)
}
