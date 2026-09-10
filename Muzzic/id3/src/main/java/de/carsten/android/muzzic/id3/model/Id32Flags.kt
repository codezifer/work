package de.carsten.android.muzzic.id3.model

import java.nio.ByteBuffer
import java.util.BitSet

class Id32Flags(private val buf: ByteBuffer) : Flags {
    private var _unsynchronisation = false
    override val unsynchronisation: Boolean = _unsynchronisation

    private var _extendedHeader = false
    override val extendedHeader: Boolean = _extendedHeader

    private var _experimentalIndicator = false
    override val experimentalIndicator: Boolean = _experimentalIndicator

    private var _footerPresent = false
    override val footerPresent: Boolean = _footerPresent

    init {

    }

    private fun parse(): BitSet {
        val bits = BitSet.valueOf()
    }
}
