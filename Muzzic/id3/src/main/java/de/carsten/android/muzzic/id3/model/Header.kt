package de.carsten.android.muzzic.id3.model

interface Header {
    val fileId: String
    val version: String
    val flags: Flags
    val size: Long
}
