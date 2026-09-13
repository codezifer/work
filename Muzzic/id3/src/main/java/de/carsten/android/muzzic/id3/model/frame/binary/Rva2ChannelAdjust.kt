package de.carsten.android.muzzic.id3.model.frame.binary

import de.carsten.android.muzzic.id3.model.tag.DataBytes

/**
 * Per-channel volume adjustment.
 *
 * The adjustment is a fixed point decibel value: signed 16 bit integer
 * representing adjustment * 512 (±64 dB, ~0.002 dB precision).
 *
 * @property channelType affected channel.
 * @property volumeAdjustment fixed point dB adjustment.
 * @property peakBits bits used for the peak volume, 0 means absent.
 * @property peakVolume peak volume bytes, null if absent.
 */
data class Rva2ChannelAdjust(val channelType: Rva2ChannelType, val volumeAdjustment: Short, val peakBits: UByte, val peakVolume: DataBytes?)
