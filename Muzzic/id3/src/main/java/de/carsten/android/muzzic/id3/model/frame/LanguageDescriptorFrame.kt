package de.carsten.android.muzzic.id3.model.frame

import de.carsten.android.muzzic.id3.model.tag.LanguageCode
import de.carsten.android.muzzic.id3.model.tag.TextEncoding

/**
 * Base of language-bearing frames with a content descriptor (COMM, USLT, SYLT).
 *
 * Several frames of the same id may exist per tag, but only one per
 * language and descriptor combination.
 */
interface LanguageDescriptorFrame : Id3Frame {
    /** Text encoding of descriptor and text. */
    val encoding: TextEncoding

    /** ISO-639-2 content language. */
    val language: LanguageCode

    /** Content descriptor, empty if unused. */
    val descriptor: String
}
