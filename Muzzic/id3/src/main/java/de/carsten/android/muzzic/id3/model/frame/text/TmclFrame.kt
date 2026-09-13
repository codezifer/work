package de.carsten.android.muzzic.id3.model.frame.text
import de.carsten.android.muzzic.id3.model.frame.FrameHeader
import de.carsten.android.muzzic.id3.model.frame.TextInformationFrame
import de.carsten.android.muzzic.id3.model.tag.TextEncoding
import de.carsten.android.muzzic.id3.model.visitor.Id3FrameVisitor

/** Musician credits list, alternating instrument/artist fields (TMCL). New in v2.4. */
data class TmclFrame(override val header: FrameHeader, override val encoding: TextEncoding, override val values: List<String>) : TextInformationFrame {
    override fun <R> accept(visitor: Id3FrameVisitor<R>): R = visitor.visitTmcl(this)
}
