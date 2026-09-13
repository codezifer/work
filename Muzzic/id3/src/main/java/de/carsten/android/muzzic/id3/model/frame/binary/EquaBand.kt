package de.carsten.android.muzzic.id3.model.frame.binary

import de.carsten.android.muzzic.id3.model.tag.DataBytes

/**
 * Single deprecated equalisation band (EQUA).
 *
 * @property increment true for increment, false for decrement.
 * @property frequency frequency with MSB replaced by the direction bit.
 * @property adjustment adjustment bytes.
 */
data class EquaBand(val increment: Boolean, val frequency: Int, val adjustment: DataBytes)
