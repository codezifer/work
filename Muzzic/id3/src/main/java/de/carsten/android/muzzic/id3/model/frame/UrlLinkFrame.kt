package de.carsten.android.muzzic.id3.model.frame

/**
 * Base of all `W000`-`WZZZ` URL link frames (excluding `WXXX`).
 *
 * URLs are always ISO-8859-1 encoded and may be relative.
 */
interface UrlLinkFrame : Id3Frame {
    /** Linked URL. */
    val url: String
}
