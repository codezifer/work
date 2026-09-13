package de.carsten.android.muzzic.id3.model.frame.language

/**
 * Single synchronised lyrics entry: text valid from the timestamp on.
 *
 * @property text syllable or text chunk.
 * @property timestamp timestamp in the unit of the frame's [de.carsten.android.muzzic.id3.model.tag.TimestampFormat].
 */
data class SyncEntry(val text: String, val timestamp: Long)
