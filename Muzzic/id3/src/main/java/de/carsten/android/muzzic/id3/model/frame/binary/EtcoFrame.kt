package de.carsten.android.muzzic.id3.model.frame.binary

import de.carsten.android.muzzic.id3.model.frame.BinaryDataFrame
import de.carsten.android.muzzic.id3.model.frame.FrameHeader
import de.carsten.android.muzzic.id3.model.tag.TimestampFormat
import de.carsten.android.muzzic.id3.model.visitor.Id3FrameVisitor

/**
 * Event timing codes for audio synchronisation (ETCO).
 *
 * Events must be chronologically ordered. At most one per tag.
 *
 * @property header frame header.
 * @property format timestamp unit.
 * @property events chronologically ordered events.
 */
data class EtcoFrame(override val header: FrameHeader, val format: TimestampFormat, val events: List<TimingEvent>) : BinaryDataFrame {
    override fun <R> accept(visitor: Id3FrameVisitor<R>): R = visitor.visitEtco(this)
}
