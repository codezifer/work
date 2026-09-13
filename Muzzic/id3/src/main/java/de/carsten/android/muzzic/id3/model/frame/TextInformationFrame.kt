package de.carsten.android.muzzic.id3.model.frame

import de.carsten.android.muzzic.id3.model.tag.TextEncoding

/**
 * Base of all `T000`-`TZZZ` text information frames (excluding `TXXX`).
 *
 * At most one frame of each kind exists per tag. The body holds an
 * encoding byte followed by a null-separated string list.
 */
interface TextInformationFrame : Id3Frame {
    /** Text encoding of all strings in this frame. */
    val encoding: TextEncoding

    /** Null-separated string list. */
    val values: List<String>
}
