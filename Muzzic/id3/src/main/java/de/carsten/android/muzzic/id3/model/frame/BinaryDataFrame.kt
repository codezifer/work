package de.carsten.android.muzzic.id3.model.frame

/**
 * Marker for binary container frames without shared fields.
 *
 * Groups all frames whose bodies follow individual layouts (pictures,
 * counters, timing codes, registrations, ...) under one visitor group
 * method so operations can handle or skip them together.
 */
interface BinaryDataFrame : Id3Frame
