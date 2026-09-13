package de.carsten.android.muzzic.id3.model.frame.binary

/**
 * Single equalisation adjustment point.
 *
 * @property frequency frequency in 1/2 Hz units (0-32767 Hz).
 * @property volumeAdjustment fixed point dB adjustment, same encoding as RVA2.
 */
data class Equ2Point(val frequency: Int, val volumeAdjustment: Short)
