package de.carsten.android.muzzic.id3.model.frame.binary

/**
 * Single event timing entry.
 *
 * @property type event type.
 * @property timestamp event time in the frame's [de.carsten.android.muzzic.id3.model.tag.TimestampFormat] unit.
 */
data class TimingEvent(val type: EventType, val timestamp: Long)
