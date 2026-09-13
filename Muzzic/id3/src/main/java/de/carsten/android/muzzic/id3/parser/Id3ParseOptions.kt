package de.carsten.android.muzzic.id3.parser

/**
 * Options tuning [Id3Parser] for the library scanner use case.
 *
 * All options default to full parsing, so existing callers are unaffected.
 *
 * @property skipPictureData when true, APIC picture bytes are not kept in
 * memory. The returned frame still carries MIME type, description and the
 * absolute [offset][de.carsten.android.muzzic.id3.model.tag.DataBytes.offset]
 * / [length][de.carsten.android.muzzic.id3.model.tag.DataBytes.length] of the
 * picture data, so consumers can lazy-load the bytes later. Skipped payloads
 * use an empty byte array — identical offsets/lengths still compare by
 * position, not by content.
 */
data class Id3ParseOptions(val skipPictureData: Boolean = false)
