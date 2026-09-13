package de.carsten.android.muzzic.id3.model.frame

/**
 * Base of frames keyed by an owner identifier URL or email (UFID, PRIV,
 * ENCR, GRID, AENC).
 */
interface OwnerIdentifierFrame : Id3Frame {
    /** Owner identifier of the issuing organisation. */
    val ownerIdentifier: String
}
