package de.carsten.android.muzzic.id3.parser

import de.carsten.android.muzzic.id3.model.frame.FrameHeader
import de.carsten.android.muzzic.id3.model.frame.FrameIds
import de.carsten.android.muzzic.id3.model.frame.Id3Frame
import de.carsten.android.muzzic.id3.model.frame.UnknownFrame
import de.carsten.android.muzzic.id3.model.tag.DataBytes

/**
 * Decodes a typed frame from a header and its plain body bytes.
 *
 * Bodies passed here are already unsynchronised-reversed; compressed or
 * encrypted bodies never reach decoders (see [Id3Parser]).
 *
 * @param bodyOffset absolute stream position of `body[0]`, used to fill
 * [DataBytes.offset][de.carsten.android.muzzic.id3.model.tag.DataBytes.offset].
 */
typealias FrameDecoder = (header: FrameHeader, body: ByteArray, bodyOffset: Int) -> Id3Frame

/**
 * Registry mapping frame identifiers to their decoders.
 *
 * Generic layouts (text, URL, user-defined, simple binary) are
 * pre-registered. Complex layouts not yet implemented fall back to
 * [UnknownFrame] so parsing
 * never fails on valid but unsupported frames.
 */
object FrameDecoderRegistry {
    private val decoders = mutableMapOf<String, FrameDecoder>()

    init {
        registerTextFrames()
        registerUrlFrames()
        register(FrameIds.TXXX, ::decodeTxxx)
        register(FrameIds.WXXX, ::decodeWxxx)
        register(FrameIds.COMM, ::decodeComm)
        register(FrameIds.USLT, ::decodeUslt)
        register(FrameIds.UFID, ::decodeUfid)
        register(FrameIds.PRIV, ::decodePriv)
        register(FrameIds.PCNT, ::decodePcnt)
        register(FrameIds.POPM, ::decodePopm)
        register(FrameIds.APIC, ::decodeApic)
        register(FrameIds.GEOB, ::decodeGeob)
        register(FrameIds.USER, ::decodeUser)
        register(FrameIds.OWNE, ::decodeOwne)
        register(FrameIds.SYLT, ::decodeSylt)
        register(FrameIds.ETCO, ::decodeEtco)
        register(FrameIds.RVA2, ::decodeRva2)
        register(FrameIds.EQU2, ::decodeEqu2)
        register(FrameIds.COMR, ::decodeComr)
        register(FrameIds.ASPI, ::decodeAspi)
    }

    /**
     * Registers a decoder for a frame identifier.
     *
     * @param frameId four-character identifier.
     * @param decoder decoder producing the typed frame.
     */
    fun register(frameId: String, decoder: FrameDecoder) {
        decoders[frameId] = decoder
    }

    /**
     * Decodes a frame body, falling back to [UnknownFrame][de.carsten.android.muzzic.id3.model.frame.UnknownFrame].
     *
     * @param header parsed frame header.
     * @param body plain frame body bytes.
     * @param bodyOffset absolute stream position of `body[0]`.
     * @return typed frame or [UnknownFrame][de.carsten.android.muzzic.id3.model.frame.UnknownFrame].
     */
    fun decode(header: FrameHeader, body: ByteArray, bodyOffset: Int = 0): Id3Frame = decoders[header.frameId.value]?.invoke(header, body, bodyOffset)
        ?: UnknownFrame(header, DataBytes(body.copyOf(), offset = bodyOffset))
}
