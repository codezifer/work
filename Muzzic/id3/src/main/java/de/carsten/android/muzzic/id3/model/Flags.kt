package de.carsten.android.muzzic.id3.model

interface Flags {
    val unsynchronisation: Boolean
    val extendedHeader: Boolean
    val experimentalIndicator: Boolean
    val footerPresent: Boolean
}
