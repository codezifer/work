package de.carsten.android.muzzic.id3.model.frame.url
import de.carsten.android.muzzic.id3.model.frame.FrameHeader
import de.carsten.android.muzzic.id3.model.frame.UrlLinkFrame
import de.carsten.android.muzzic.id3.model.visitor.Id3FrameVisitor

/** Official internet radio station homepage URL (WORS). */
data class WorsFrame(override val header: FrameHeader, override val url: String) : UrlLinkFrame {
    override fun <R> accept(visitor: Id3FrameVisitor<R>): R = visitor.visitWors(this)
}
