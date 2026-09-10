package de.carsten.android.muzzic.id3.model

import java.io.InputStream

class Id323Header(private val input: InputStream) : Header {
    private var _fileId: String = ""
    override val fileId: String = _fileId

    private var _version: String = ""
    override val version: String = _version

    private var _flag: Flags? = null
    override val flags: Flags

    val size: Long
}
