package de.carsten.android.muzzic.id3.model.frame.text
import de.carsten.android.muzzic.id3.model.frame.FrameHeader
import de.carsten.android.muzzic.id3.model.frame.TextInformationFrame
import de.carsten.android.muzzic.id3.model.tag.TextEncoding
import de.carsten.android.muzzic.id3.model.visitor.Id3FrameVisitor

/** Length in milliseconds, numeric string (TLEN). */
data class TlenFrame(override val header: FrameHeader, override val encoding: TextEncoding, override val values: List<String>) : TextInformationFrame {
    override fun <R> accept(visitor: Id3FrameVisitor<R>): R = visitor.visitTlen(this)
}
