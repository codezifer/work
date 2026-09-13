package de.carsten.android.muzzic.id3.parser

import de.carsten.android.muzzic.id3.model.frame.FrameHeader
import de.carsten.android.muzzic.id3.model.frame.FrameIds
import de.carsten.android.muzzic.id3.model.frame.Id3Frame
import de.carsten.android.muzzic.id3.model.frame.TextInformationFrame
import de.carsten.android.muzzic.id3.model.frame.UnknownFrame
import de.carsten.android.muzzic.id3.model.frame.UrlLinkFrame
import de.carsten.android.muzzic.id3.model.frame.binary.ApicFrame
import de.carsten.android.muzzic.id3.model.frame.binary.AspiFrame
import de.carsten.android.muzzic.id3.model.frame.binary.ComrFrame
import de.carsten.android.muzzic.id3.model.frame.binary.Equ2Frame
import de.carsten.android.muzzic.id3.model.frame.binary.Equ2Interpolation
import de.carsten.android.muzzic.id3.model.frame.binary.Equ2Point
import de.carsten.android.muzzic.id3.model.frame.binary.EtcoFrame
import de.carsten.android.muzzic.id3.model.frame.binary.EventType
import de.carsten.android.muzzic.id3.model.frame.binary.GeobFrame
import de.carsten.android.muzzic.id3.model.frame.binary.OwneFrame
import de.carsten.android.muzzic.id3.model.frame.binary.PcntFrame
import de.carsten.android.muzzic.id3.model.frame.binary.PictureType
import de.carsten.android.muzzic.id3.model.frame.binary.PopmFrame
import de.carsten.android.muzzic.id3.model.frame.binary.PrivFrame
import de.carsten.android.muzzic.id3.model.frame.binary.ReceivedAs
import de.carsten.android.muzzic.id3.model.frame.binary.Rva2ChannelAdjust
import de.carsten.android.muzzic.id3.model.frame.binary.Rva2ChannelType
import de.carsten.android.muzzic.id3.model.frame.binary.Rva2Frame
import de.carsten.android.muzzic.id3.model.frame.binary.TimingEvent
import de.carsten.android.muzzic.id3.model.frame.binary.UfidFrame
import de.carsten.android.muzzic.id3.model.frame.language.CommFrame
import de.carsten.android.muzzic.id3.model.frame.language.SyltContentType
import de.carsten.android.muzzic.id3.model.frame.language.SyltFrame
import de.carsten.android.muzzic.id3.model.frame.language.SyncEntry
import de.carsten.android.muzzic.id3.model.frame.language.UserFrame
import de.carsten.android.muzzic.id3.model.frame.language.UsltFrame
import de.carsten.android.muzzic.id3.model.frame.text.TalbFrame
import de.carsten.android.muzzic.id3.model.frame.text.TbpmFrame
import de.carsten.android.muzzic.id3.model.frame.text.TcomFrame
import de.carsten.android.muzzic.id3.model.frame.text.TconFrame
import de.carsten.android.muzzic.id3.model.frame.text.TcopFrame
import de.carsten.android.muzzic.id3.model.frame.text.TdatFrame
import de.carsten.android.muzzic.id3.model.frame.text.TdenFrame
import de.carsten.android.muzzic.id3.model.frame.text.TdlyFrame
import de.carsten.android.muzzic.id3.model.frame.text.TdorFrame
import de.carsten.android.muzzic.id3.model.frame.text.TdrcFrame
import de.carsten.android.muzzic.id3.model.frame.text.TdrlFrame
import de.carsten.android.muzzic.id3.model.frame.text.TdtgFrame
import de.carsten.android.muzzic.id3.model.frame.text.TencFrame
import de.carsten.android.muzzic.id3.model.frame.text.TextFrame
import de.carsten.android.muzzic.id3.model.frame.text.TfltFrame
import de.carsten.android.muzzic.id3.model.frame.text.TimeFrame
import de.carsten.android.muzzic.id3.model.frame.text.TiplFrame
import de.carsten.android.muzzic.id3.model.frame.text.Tit1Frame
import de.carsten.android.muzzic.id3.model.frame.text.Tit2Frame
import de.carsten.android.muzzic.id3.model.frame.text.Tit3Frame
import de.carsten.android.muzzic.id3.model.frame.text.TkeyFrame
import de.carsten.android.muzzic.id3.model.frame.text.TlanFrame
import de.carsten.android.muzzic.id3.model.frame.text.TlenFrame
import de.carsten.android.muzzic.id3.model.frame.text.TmclFrame
import de.carsten.android.muzzic.id3.model.frame.text.TmedFrame
import de.carsten.android.muzzic.id3.model.frame.text.TmooFrame
import de.carsten.android.muzzic.id3.model.frame.text.ToalFrame
import de.carsten.android.muzzic.id3.model.frame.text.TofnFrame
import de.carsten.android.muzzic.id3.model.frame.text.TolyFrame
import de.carsten.android.muzzic.id3.model.frame.text.TopeFrame
import de.carsten.android.muzzic.id3.model.frame.text.ToryFrame
import de.carsten.android.muzzic.id3.model.frame.text.TownFrame
import de.carsten.android.muzzic.id3.model.frame.text.Tpe1Frame
import de.carsten.android.muzzic.id3.model.frame.text.Tpe2Frame
import de.carsten.android.muzzic.id3.model.frame.text.Tpe3Frame
import de.carsten.android.muzzic.id3.model.frame.text.Tpe4Frame
import de.carsten.android.muzzic.id3.model.frame.text.TposFrame
import de.carsten.android.muzzic.id3.model.frame.text.TproFrame
import de.carsten.android.muzzic.id3.model.frame.text.TpubFrame
import de.carsten.android.muzzic.id3.model.frame.text.TrckFrame
import de.carsten.android.muzzic.id3.model.frame.text.TrdaFrame
import de.carsten.android.muzzic.id3.model.frame.text.TrsnFrame
import de.carsten.android.muzzic.id3.model.frame.text.TrsoFrame
import de.carsten.android.muzzic.id3.model.frame.text.TsizFrame
import de.carsten.android.muzzic.id3.model.frame.text.TsoaFrame
import de.carsten.android.muzzic.id3.model.frame.text.TsopFrame
import de.carsten.android.muzzic.id3.model.frame.text.TsotFrame
import de.carsten.android.muzzic.id3.model.frame.text.TsrcFrame
import de.carsten.android.muzzic.id3.model.frame.text.TsseFrame
import de.carsten.android.muzzic.id3.model.frame.text.TsstFrame
import de.carsten.android.muzzic.id3.model.frame.text.TxxxFrame
import de.carsten.android.muzzic.id3.model.frame.text.TyerFrame
import de.carsten.android.muzzic.id3.model.frame.url.WcomFrame
import de.carsten.android.muzzic.id3.model.frame.url.WcopFrame
import de.carsten.android.muzzic.id3.model.frame.url.WoafFrame
import de.carsten.android.muzzic.id3.model.frame.url.WoarFrame
import de.carsten.android.muzzic.id3.model.frame.url.WoasFrame
import de.carsten.android.muzzic.id3.model.frame.url.WorsFrame
import de.carsten.android.muzzic.id3.model.frame.url.WpayFrame
import de.carsten.android.muzzic.id3.model.frame.url.WpubFrame
import de.carsten.android.muzzic.id3.model.frame.url.WxxxFrame
import de.carsten.android.muzzic.id3.model.tag.DataBytes
import de.carsten.android.muzzic.id3.model.tag.LanguageCode
import de.carsten.android.muzzic.id3.model.tag.TextEncoding
import de.carsten.android.muzzic.id3.model.tag.TimestampFormat
import java.nio.charset.Charset

private typealias TextFactory = (FrameHeader, TextEncoding, List<String>) -> TextInformationFrame
private typealias UrlFactory = (FrameHeader, String) -> UrlLinkFrame

// Resolved once here instead of per frame: Charset.forName() used to run
// on every text segment in the hot path (see TextEncoding.charset).
private val ISO_8859_1 = TextEncoding.ISO_8859_1.charset

/** Timestamp field size in ETCO and SYLT entries, matching the "32 bit sized" format description. */
private const val TIMESTAMP_SIZE = 4

/** Minimum SYLT body size: encoding + language (3) + format + content type. */
private const val SYLT_MIN_SIZE = 6

/** Fixed YYYYMMDD date field size in OWNE and COMR frames. */
private const val OWNE_DATE_LENGTH = 8
private const val COMR_DATE_LENGTH = 8

/** Per-channel head size in RVA2 entries: type + adjustment (2) + peak bits. */
private const val RVA2_CHANNEL_HEAD_SIZE = 4

/** Adjustment point size in EQU2 entries: frequency (2) + adjustment (2). */
private const val EQU2_POINT_SIZE = 4

/** ASPI head size: start (4) + length (4) + count (2) + bits per point (1). */
private const val ASPI_HEAD_SIZE = 11

/** ETCO escape byte introducing the actual event type in the next byte. */
private const val ESCAPE_BYTE: UByte = 255u

/** ETCO padding event type without meaning. */
private const val PADDING_BYTE: UByte = 0u

/**
 * Registers all plain `T***` text frame decoders.
 */
internal fun FrameDecoderRegistry.registerTextFrames() {
    val factories: Map<String, TextFactory> = mapOf(
        FrameIds.TALB to ::TalbFrame,
        FrameIds.TBPM to ::TbpmFrame,
        FrameIds.TCOM to ::TcomFrame,
        FrameIds.TCON to ::TconFrame,
        FrameIds.TCOP to ::TcopFrame,
        FrameIds.TDEN to ::TdenFrame,
        FrameIds.TDLY to ::TdlyFrame,
        FrameIds.TDOR to ::TdorFrame,
        FrameIds.TDRC to ::TdrcFrame,
        FrameIds.TDRL to ::TdrlFrame,
        FrameIds.TDTG to ::TdtgFrame,
        FrameIds.TENC to ::TencFrame,
        FrameIds.TEXT to ::TextFrame,
        FrameIds.TFLT to ::TfltFrame,
        FrameIds.TIPL to ::TiplFrame,
        FrameIds.TIT1 to ::Tit1Frame,
        FrameIds.TIT2 to ::Tit2Frame,
        FrameIds.TIT3 to ::Tit3Frame,
        FrameIds.TKEY to ::TkeyFrame,
        FrameIds.TLAN to ::TlanFrame,
        FrameIds.TLEN to ::TlenFrame,
        FrameIds.TMCL to ::TmclFrame,
        FrameIds.TMED to ::TmedFrame,
        FrameIds.TMOO to ::TmooFrame,
        FrameIds.TOAL to ::ToalFrame,
        FrameIds.TOFN to ::TofnFrame,
        FrameIds.TOLY to ::TolyFrame,
        FrameIds.TOPE to ::TopeFrame,
        FrameIds.TOWN to ::TownFrame,
        FrameIds.TPE1 to ::Tpe1Frame,
        FrameIds.TPE2 to ::Tpe2Frame,
        FrameIds.TPE3 to ::Tpe3Frame,
        FrameIds.TPE4 to ::Tpe4Frame,
        FrameIds.TPOS to ::TposFrame,
        FrameIds.TPRO to ::TproFrame,
        FrameIds.TPUB to ::TpubFrame,
        FrameIds.TRCK to ::TrckFrame,
        FrameIds.TRSN to ::TrsnFrame,
        FrameIds.TRSO to ::TrsoFrame,
        FrameIds.TSOA to ::TsoaFrame,
        FrameIds.TSOP to ::TsopFrame,
        FrameIds.TSOT to ::TsotFrame,
        FrameIds.TSRC to ::TsrcFrame,
        FrameIds.TSSE to ::TsseFrame,
        FrameIds.TSST to ::TsstFrame,
        FrameIds.TDAT to ::TdatFrame,
        FrameIds.TIME to ::TimeFrame,
        FrameIds.TORY to ::ToryFrame,
        FrameIds.TRDA to ::TrdaFrame,
        FrameIds.TSIZ to ::TsizFrame,
        FrameIds.TYER to ::TyerFrame,
    )
    factories.forEach { (id, factory) ->
        register(id) { header, body, _ ->
            val (encoding, values) = decodeTextBody(body)
            factory(header, encoding, values)
        }
    }
}

/**
 * Registers all plain `W***` URL frame decoders.
 */
internal fun FrameDecoderRegistry.registerUrlFrames() {
    val factories: Map<String, UrlFactory> = mapOf(
        FrameIds.WCOM to ::WcomFrame,
        FrameIds.WCOP to ::WcopFrame,
        FrameIds.WOAF to ::WoafFrame,
        FrameIds.WOAR to ::WoarFrame,
        FrameIds.WOAS to ::WoasFrame,
        FrameIds.WORS to ::WorsFrame,
        FrameIds.WPAY to ::WpayFrame,
        FrameIds.WPUB to ::WpubFrame,
    )
    factories.forEach { (id, factory) ->
        register(id) { header, body, _ -> factory(header, decodeUrlBody(body)) }
    }
}

/**
 * Decodes a TXXX user defined text frame.
 *
 * @param header frame header.
 * @param body plain body bytes.
 * @param bodyOffset absolute stream position of `body[0]`.
 * @return decoded frame.
 */
internal fun decodeTxxx(header: FrameHeader, body: ByteArray, bodyOffset: Int): Id3Frame {
    val encoding = encodingOf(body)
    val (description, valueOffset) = readEncodedString(body, 1, encoding)
    return TxxxFrame(header, encoding, description, decodeString(body, valueOffset, body.size, encoding))
}

/**
 * Decodes a WXXX user defined URL frame.
 *
 * @param header frame header.
 * @param body plain body bytes.
 * @param bodyOffset absolute stream position of `body[0]`.
 * @return decoded frame.
 */
internal fun decodeWxxx(header: FrameHeader, body: ByteArray, bodyOffset: Int): Id3Frame {
    val encoding = encodingOf(body)
    val (description, urlOffset) = readEncodedString(body, 1, encoding)
    return WxxxFrame(header, encoding, description, decodeUrlBody(body, urlOffset, body.size))
}

/**
 * Decodes a COMM comment frame.
 *
 * @param header frame header.
 * @param body plain body bytes.
 * @param bodyOffset absolute stream position of `body[0]`.
 * @return decoded frame.
 */
internal fun decodeComm(header: FrameHeader, body: ByteArray, bodyOffset: Int): Id3Frame {
    val encoding = encodingOf(body)
    val language = languageOf(body)
    val (descriptor, textOffset) = readEncodedString(body, 4, encoding)
    return CommFrame(header, encoding, language, descriptor, decodeString(body, textOffset, body.size, encoding))
}

/**
 * Decodes a USLT unsynchronised lyrics frame.
 *
 * @param header frame header.
 * @param body plain body bytes.
 * @param bodyOffset absolute stream position of `body[0]`.
 * @return decoded frame.
 */
internal fun decodeUslt(header: FrameHeader, body: ByteArray, bodyOffset: Int): Id3Frame {
    val encoding = encodingOf(body)
    val language = languageOf(body)
    val (descriptor, textOffset) = readEncodedString(body, 4, encoding)
    return UsltFrame(header, encoding, language, descriptor, decodeString(body, textOffset, body.size, encoding))
}

/**
 * Decodes a UFID unique file identifier frame.
 *
 * @param header frame header.
 * @param body plain body bytes.
 * @param bodyOffset absolute stream position of `body[0]`.
 * @return decoded frame.
 */
internal fun decodeUfid(header: FrameHeader, body: ByteArray, bodyOffset: Int): Id3Frame {
    val (owner, dataOffset) = readIsoString(body, 0)
    return UfidFrame(header, owner, DataBytes(body.copyOfRange(dataOffset, body.size), offset = bodyOffset + dataOffset))
}

/**
 * Decodes a PRIV private frame.
 *
 * @param header frame header.
 * @param body plain body bytes.
 * @param bodyOffset absolute stream position of `body[0]`.
 * @return decoded frame.
 */
internal fun decodePriv(header: FrameHeader, body: ByteArray, bodyOffset: Int): Id3Frame {
    val (owner, dataOffset) = readIsoString(body, 0)
    return PrivFrame(header, owner, DataBytes(body.copyOfRange(dataOffset, body.size), offset = bodyOffset + dataOffset))
}

/**
 * Decodes a PCNT play counter frame.
 *
 * @param header frame header.
 * @param body plain body bytes.
 * @param bodyOffset absolute stream position of `body[0]`.
 * @return decoded frame.
 */
internal fun decodePcnt(header: FrameHeader, body: ByteArray, bodyOffset: Int): Id3Frame = PcntFrame(header, DataBytes(body, offset = bodyOffset))

/**
 * Decodes a POPM popularimeter frame.
 *
 * @param header frame header.
 * @param body plain body bytes.
 * @param bodyOffset absolute stream position of `body[0]`.
 * @return decoded frame.
 */
internal fun decodePopm(header: FrameHeader, body: ByteArray, bodyOffset: Int): Id3Frame {
    val (email, ratingOffset) = readIsoString(body, 0)
    if (ratingOffset >= body.size) return PopmFrame(header, email, 0u, null)
    val rating = body[ratingOffset].toUByte()
    val counter = body.copyOfRange(ratingOffset + 1, body.size).takeIf { it.isNotEmpty() }?.let {
        DataBytes(it, offset = bodyOffset + ratingOffset + 1)
    }
    return PopmFrame(header, email, rating, counter)
}

/**
 * Decodes an APIC attached picture frame (§4.14).
 *
 * Layout: encoding + MIME type (ISO-8859-1, terminated) + picture type +
 * description (encoding, terminated) + picture data (rest, may be a linked
 * URL when the MIME type is "-->").
 *
 * @param header frame header.
 * @param body plain body bytes.
 * @param bodyOffset absolute stream position of `body[0]`.
 * @return decoded frame, or [UnknownFrame] when truncated.
 */
internal fun decodeApic(header: FrameHeader, body: ByteArray, bodyOffset: Int): Id3Frame {
    val encoding = encodingOf(body)
    val (mimeType, typeOffset) = readIsoString(body, 1)
    if (typeOffset >= body.size) return UnknownFrame(header, DataBytes(body, offset = bodyOffset))
    val pictureType = PictureType.fromCode(body[typeOffset])
    val (description, dataOffset) = readEncodedString(body, typeOffset + 1, encoding)
    val data = DataBytes(body.copyOfRange(dataOffset, body.size), offset = bodyOffset + dataOffset)
    return ApicFrame(header, encoding, mimeType, pictureType, description, data)
}

/**
 * Decodes an APIC frame without keeping the picture bytes in memory.
 *
 * MIME type, picture type and description are parsed normally; the picture
 * payload is replaced by an empty array that still carries the absolute
 * stream [offset][DataBytes.offset] and [length][DataBytes.length], so
 * consumers can lazy-load the bytes later (see [Id3ParseOptions]).
 *
 * @param header frame header.
 * @param body plain body bytes.
 * @param bodyOffset absolute stream position of `body[0]`.
 * @return decoded frame with empty picture payload, or [UnknownFrame] when truncated.
 */
internal fun decodeApicSkipped(header: FrameHeader, body: ByteArray, bodyOffset: Int): Id3Frame {
    val encoding = encodingOf(body)
    val (mimeType, typeOffset) = readIsoString(body, 1)
    if (typeOffset >= body.size) return UnknownFrame(header, DataBytes(body, offset = bodyOffset))
    val pictureType = PictureType.fromCode(body[typeOffset])
    val (description, dataOffset) = readEncodedString(body, typeOffset + 1, encoding)
    val skipped = DataBytes(byteArrayOf(), offset = bodyOffset + dataOffset, length = body.size - dataOffset)
    return ApicFrame(header, encoding, mimeType, pictureType, description, skipped)
}

/**
 * Decodes a GEOB general encapsulated object frame (§4.15).
 *
 * Layout: encoding + MIME type (ISO-8859-1, terminated) + filename
 * (encoding, terminated) + description (encoding, terminated) + object
 * (rest). MIME type and filename may be empty except for their
 * terminators.
 *
 * @param header frame header.
 * @param body plain body bytes.
 * @param bodyOffset absolute stream position of `body[0]`.
 * @return decoded frame.
 */
internal fun decodeGeob(header: FrameHeader, body: ByteArray, bodyOffset: Int): Id3Frame {
    val encoding = encodingOf(body)
    val (mimeType, filenameOffset) = readIsoString(body, 1)
    val (filename, descriptionOffset) = readEncodedString(body, filenameOffset, encoding)
    val (description, dataOffset) = readEncodedString(body, descriptionOffset, encoding)
    val data = DataBytes(body.copyOfRange(dataOffset, body.size), offset = bodyOffset + dataOffset)
    return GeobFrame(header, encoding, mimeType, filename, description, data)
}

/**
 * Decodes a USER terms of use frame (§4.22).
 *
 * Layout: encoding + language (3 bytes) + text (rest, newlines allowed).
 *
 * @param header frame header.
 * @param body plain body bytes.
 * @param bodyOffset absolute stream position of `body[0]`.
 * @return decoded frame.
 */
internal fun decodeUser(header: FrameHeader, body: ByteArray, bodyOffset: Int): Id3Frame {
    val encoding = encodingOf(body)
    val language = languageOf(body)
    return UserFrame(header, encoding, language, decodeString(body, 4, body.size, encoding))
}

/**
 * Decodes an OWNE ownership frame (§4.23).
 *
 * Layout: encoding + price paid (ISO-8859-1, terminated) + purchase date
 * (fixed 8 bytes, YYYYMMDD, not terminated) + seller (rest, encoding).
 *
 * @param header frame header.
 * @param body plain body bytes.
 * @param bodyOffset absolute stream position of `body[0]`.
 * @return decoded frame, or [UnknownFrame] when truncated.
 */
internal fun decodeOwne(header: FrameHeader, body: ByteArray, bodyOffset: Int): Id3Frame {
    val encoding = encodingOf(body)
    val (pricePaid, dateOffset) = readIsoString(body, 1)
    if (dateOffset + OWNE_DATE_LENGTH > body.size) {
        return UnknownFrame(header, DataBytes(body, offset = bodyOffset))
    }
    val purchaseDate = String(body, dateOffset, OWNE_DATE_LENGTH, ISO_8859_1)
    val seller = decodeString(body, dateOffset + OWNE_DATE_LENGTH, body.size, encoding)
    return OwneFrame(header, encoding, pricePaid, purchaseDate, seller)
}

/**
 * Decodes a SYLT synchronised lyrics/text frame (§4.9).
 *
 * Layout: encoding + language (3 bytes) + timestamp format + content type
 * + descriptor (encoding, terminated) + repeated entries of text
 * (encoding, terminated) + timestamp (4 bytes big-endian, matching the
 * "32 bit sized" format description). Parsing stops tolerantly at a
 * truncated tail.
 *
 * @param header frame header.
 * @param body plain body bytes.
 * @param bodyOffset absolute stream position of `body[0]`.
 * @return decoded frame, or [UnknownFrame] for unknown formats or truncation.
 */
internal fun decodeSylt(header: FrameHeader, body: ByteArray, bodyOffset: Int): Id3Frame {
    if (body.size < SYLT_MIN_SIZE) return UnknownFrame(header, DataBytes(body, offset = bodyOffset))
    val encoding = encodingOf(body)
    val language = languageOf(body)
    val format = TimestampFormat.fromCode(body[4])
        ?: return UnknownFrame(header, DataBytes(body, offset = bodyOffset))
    val contentType = SyltContentType.fromCode(body[5])
    val (descriptor, entryOffset) = readEncodedString(body, 6, encoding)
    val entries = mutableListOf<SyncEntry>()
    var index = entryOffset
    while (index < body.size) {
        val (text, stampOffset) = readEncodedString(body, index, encoding)
        if (stampOffset + TIMESTAMP_SIZE > body.size) break
        entries.add(SyncEntry(text, readU32BE(body, stampOffset)))
        index = stampOffset + TIMESTAMP_SIZE
    }
    return SyltFrame(header, encoding, language, descriptor, format, contentType, entries)
}

/**
 * Decodes an ETCO event timing codes frame (§4.5).
 *
 * Layout: timestamp format + repeated entries of event type + timestamp
 * (4 bytes big-endian, matching the "32 bit sized" format description).
 * `$00` padding events are skipped, `$FF` escapes to the following byte
 * as the actual type. Unknown event codes fall back to [UnknownFrame] so
 * no bytes are lost.
 *
 * @param header frame header.
 * @param body plain body bytes.
 * @param bodyOffset absolute stream position of `body[0]`.
 * @return decoded frame, or [UnknownFrame] for unknown formats or codes.
 */
internal fun decodeEtco(header: FrameHeader, body: ByteArray, bodyOffset: Int): Id3Frame {
    if (body.isEmpty()) return UnknownFrame(header, DataBytes(body, offset = bodyOffset))
    val format = TimestampFormat.fromCode(body[0])
        ?: return UnknownFrame(header, DataBytes(body, offset = bodyOffset))
    val events = mutableListOf<TimingEvent>()
    var index = 1
    while (index < body.size) {
        var code = body[index++].toUByte()
        while (code == ESCAPE_BYTE && index < body.size) code = body[index++].toUByte()
        if (code == ESCAPE_BYTE || code == PADDING_BYTE) {
            if (code == PADDING_BYTE && index + TIMESTAMP_SIZE <= body.size) index += TIMESTAMP_SIZE
            continue
        }
        val type = EventType.fromCode(code.toByte())
            ?: return UnknownFrame(header, DataBytes(body, offset = bodyOffset))
        if (index + TIMESTAMP_SIZE > body.size) break
        events.add(TimingEvent(type, readU32BE(body, index)))
        index += TIMESTAMP_SIZE
    }
    return EtcoFrame(header, format, events)
}

/**
 * Decodes an RVA2 relative volume adjustment frame (§4.11).
 *
 * Layout: identification (ISO-8859-1, terminated) + repeated per-channel
 * entries of channel type + fixed-point dB adjustment (2 bytes signed
 * big-endian, value × 512) + peak bits + peak volume (`ceil(bits/8)`
 * bytes, absent when bits is 0).
 *
 * @param header frame header.
 * @param body plain body bytes.
 * @param bodyOffset absolute stream position of `body[0]`.
 * @return decoded frame, or [UnknownFrame] when truncated.
 */
internal fun decodeRva2(header: FrameHeader, body: ByteArray, bodyOffset: Int): Id3Frame {
    val (identification, channelOffset) = readIsoString(body, 0)
    val channels = mutableListOf<Rva2ChannelAdjust>()
    var index = channelOffset
    while (index < body.size) {
        if (index + RVA2_CHANNEL_HEAD_SIZE > body.size) {
            return UnknownFrame(header, DataBytes(body, offset = bodyOffset))
        }
        val channelType = Rva2ChannelType.fromCode(body[index])
        val adjustment = readI16BE(body, index + 1)
        val peakBits = body[index + 3].toUByte()
        index += RVA2_CHANNEL_HEAD_SIZE
        val peakBytes = (peakBits.toInt() + Byte.SIZE_BITS - 1) / Byte.SIZE_BITS
        if (peakBytes > 0) {
            if (index + peakBytes > body.size) {
                return UnknownFrame(header, DataBytes(body, offset = bodyOffset))
            }
            val peak = DataBytes(body.copyOfRange(index, index + peakBytes), offset = bodyOffset + index)
            index += peakBytes
            channels.add(Rva2ChannelAdjust(channelType, adjustment, peakBits, peak))
        } else {
            channels.add(Rva2ChannelAdjust(channelType, adjustment, peakBits, null))
        }
    }
    return Rva2Frame(header, identification, channels)
}

/**
 * Decodes an EQU2 equalisation frame (§4.12).
 *
 * Layout: interpolation method + identification (ISO-8859-1, terminated)
 * + repeated points of frequency (2 bytes big-endian, ½ Hz units) +
 * fixed-point dB adjustment (2 bytes signed big-endian, value × 512).
 *
 * @param header frame header.
 * @param body plain body bytes.
 * @param bodyOffset absolute stream position of `body[0]`.
 * @return decoded frame, or [UnknownFrame] when truncated.
 */
internal fun decodeEqu2(header: FrameHeader, body: ByteArray, bodyOffset: Int): Id3Frame {
    if (body.isEmpty()) return UnknownFrame(header, DataBytes(body, offset = bodyOffset))
    val interpolation = Equ2Interpolation.fromCode(body[0])
    val (identification, pointOffset) = readIsoString(body, 1)
    val points = mutableListOf<Equ2Point>()
    var index = pointOffset
    while (index < body.size) {
        if (index + EQU2_POINT_SIZE > body.size) {
            return UnknownFrame(header, DataBytes(body, offset = bodyOffset))
        }
        points.add(Equ2Point(readU16BE(body, index), readI16BE(body, index + 2)))
        index += EQU2_POINT_SIZE
    }
    return Equ2Frame(header, interpolation, identification, points)
}

/**
 * Decodes a COMR commercial frame (§4.24).
 *
 * Layout: encoding + price string (ISO-8859-1, terminated) + valid-until
 * date (fixed 8 bytes, YYYYMMDD, not terminated) + contact URL
 * (ISO-8859-1, terminated) + received-as byte + seller name (encoding,
 * terminated) + description (encoding, terminated) + optional logo of
 * MIME type (ISO-8859-1, terminated) + logo bytes (rest).
 *
 * @param header frame header.
 * @param body plain body bytes.
 * @param bodyOffset absolute stream position of `body[0]`.
 * @return decoded frame, or [UnknownFrame] when truncated.
 */
internal fun decodeComr(header: FrameHeader, body: ByteArray, bodyOffset: Int): Id3Frame {
    val encoding = encodingOf(body)
    val (priceString, dateOffset) = readIsoString(body, 1)
    if (dateOffset + COMR_DATE_LENGTH > body.size) {
        return UnknownFrame(header, DataBytes(body, offset = bodyOffset))
    }
    val validUntil = String(body, dateOffset, COMR_DATE_LENGTH, ISO_8859_1)
    val (contactUrl, receivedOffset) = readIsoString(body, dateOffset + COMR_DATE_LENGTH)
    if (receivedOffset >= body.size) {
        return UnknownFrame(header, DataBytes(body, offset = bodyOffset))
    }
    val receivedAs = ReceivedAs.fromCode(body[receivedOffset])
    val (sellerName, descriptionOffset) = readEncodedString(body, receivedOffset + 1, encoding)
    val (description, logoOffset) = readEncodedString(body, descriptionOffset, encoding)
    if (logoOffset >= body.size) {
        return ComrFrame(header, encoding, priceString, validUntil, contactUrl, receivedAs, sellerName, description, null, null)
    }
    val (mimeType, logoStart) = readIsoString(body, logoOffset)
    val logo = DataBytes(body.copyOfRange(logoStart, body.size), offset = bodyOffset + logoStart)
        .takeIf { logoStart < body.size }
    return ComrFrame(header, encoding, priceString, validUntil, contactUrl, receivedAs, sellerName, description, mimeType, logo)
}

/**
 * Decodes an ASPI audio seek point index frame (§4.30).
 *
 * Layout: indexed data start and length (4 bytes each) + index point
 * count (2 bytes) + bits per point (8 or 16) + fractions of `bits/8`
 * bytes each. Other precisions fall back to [UnknownFrame].
 *
 * @param header frame header.
 * @param body plain body bytes.
 * @param bodyOffset absolute stream position of `body[0]`.
 * @return decoded frame, or [UnknownFrame] for unsupported layouts.
 */
internal fun decodeAspi(header: FrameHeader, body: ByteArray, bodyOffset: Int): Id3Frame {
    if (body.size < ASPI_HEAD_SIZE) return UnknownFrame(header, DataBytes(body, offset = bodyOffset))
    val start = readU32BE(body, 0).toUInt()
    val length = readU32BE(body, 4).toUInt()
    val count = readU16BE(body, 8)
    val bits = body[10].toUByte()
    val pointBytes = when (bits.toInt()) {
        Byte.SIZE_BITS -> 1
        Short.SIZE_BITS -> 2
        else -> return UnknownFrame(header, DataBytes(body, offset = bodyOffset))
    }
    if (ASPI_HEAD_SIZE + count * pointBytes > body.size) {
        return UnknownFrame(header, DataBytes(body, offset = bodyOffset))
    }
    val fractions = List(count) { point ->
        val offset = ASPI_HEAD_SIZE + point * pointBytes
        if (pointBytes == 1) body[offset].toInt() and 0xFF else readU16BE(body, offset)
    }
    return AspiFrame(header, start, length, fractions, bits)
}

private fun decodeTextBody(body: ByteArray): Pair<TextEncoding, List<String>> {
    if (body.isEmpty()) return TextEncoding.ISO_8859_1 to emptyList()
    val encoding = encodingOf(body)
    return encoding to splitStrings(body, 1, encoding)
}

private fun decodeUrlBody(body: ByteArray): String = decodeUrlBody(body, 0, body.size)

private fun decodeUrlBody(bytes: ByteArray, start: Int, end: Int): String {
    if (start >= end) return ""
    var stop = start
    while (stop < end && bytes[stop] != 0x00.toByte()) stop += 1
    return String(bytes, start, stop - start, ISO_8859_1)
}

private fun encodingOf(body: ByteArray): TextEncoding = if (body.isEmpty()) TextEncoding.ISO_8859_1 else TextEncoding.fromCode(body[0]) ?: TextEncoding.ISO_8859_1

private fun languageOf(body: ByteArray): LanguageCode {
    if (body.size < 4) return LanguageCode.UNKNOWN
    return LanguageCode(String(body, 1, 3, ISO_8859_1))
}

private fun splitStrings(bytes: ByteArray, offset: Int, encoding: TextEncoding): List<String> {
    if (offset >= bytes.size) return emptyList()
    val charset = encoding.charset
    val terminator = encoding.terminator
    // Multi-byte terminators (UTF-16) are scanned encoding-aligned so a
    // `00 00` pair inside a character can never match as terminator.
    val step = terminator.size
    val result = mutableListOf<String>()
    var start = offset
    var index = offset
    while (index <= bytes.size - terminator.size) {
        if (matchesAt(bytes, index, terminator)) {
            result.add(decodeSegment(bytes, start, index, charset))
            index += terminator.size
            start = index
        } else {
            index += step
        }
    }
    if (start < bytes.size && bytes.size - start >= terminator.size) {
        result.add(decodeSegment(bytes, start, bytes.size, charset))
    }
    if (result.size > 1 && result.last().isEmpty()) result.removeAt(result.lastIndex)
    return result
}

private fun readEncodedString(bytes: ByteArray, offset: Int, encoding: TextEncoding): Pair<String, Int> {
    if (offset >= bytes.size) return "" to bytes.size
    val terminator = encoding.terminator
    val step = terminator.size
    var index = offset
    while (index <= bytes.size - terminator.size && !matchesAt(bytes, index, terminator)) index += step
    val value = decodeSegment(bytes, offset, index, encoding.charset)
    return value to (index + terminator.size).coerceAtMost(bytes.size)
}

private fun readIsoString(bytes: ByteArray, offset: Int): Pair<String, Int> {
    if (offset >= bytes.size) return "" to bytes.size
    var index = offset
    while (index < bytes.size && bytes[index] != 0x00.toByte()) index += 1
    return String(bytes, offset, index - offset, ISO_8859_1) to (index + 1).coerceAtMost(bytes.size)
}

private fun decodeString(bytes: ByteArray, start: Int, end: Int, encoding: TextEncoding): String {
    if (start >= end) return ""
    return decodeSegment(bytes, start, end, encoding.charset)
}

private fun decodeSegment(bytes: ByteArray, start: Int, end: Int, charset: Charset): String {
    if (start >= end) return ""
    return String(bytes, start, end - start, charset)
}

/**
 * Reads an unsigned 16 bit big-endian integer.
 *
 * Callers must guarantee `offset + 2 <= bytes.size`.
 *
 * @param bytes source bytes.
 * @param offset start offset.
 * @return unsigned value as [Int].
 */
private fun readU16BE(bytes: ByteArray, offset: Int): Int = (bytes[offset].toInt() and 0xFF shl 8) or (bytes[offset + 1].toInt() and 0xFF)

/**
 * Reads a signed 16 bit big-endian integer.
 *
 * Callers must guarantee `offset + 2 <= bytes.size`.
 *
 * @param bytes source bytes.
 * @param offset start offset.
 * @return signed value.
 */
private fun readI16BE(bytes: ByteArray, offset: Int): Short = ((bytes[offset].toInt() and 0xFF shl 8) or (bytes[offset + 1].toInt() and 0xFF)).toShort()

/**
 * Reads an unsigned 32 bit big-endian integer.
 *
 * Callers must guarantee `offset + 4 <= bytes.size`.
 *
 * @param bytes source bytes.
 * @param offset start offset.
 * @return unsigned value as [Long].
 */
private fun readU32BE(bytes: ByteArray, offset: Int): Long = (bytes[offset].toLong() and 0xFF shl 24) or
    (bytes[offset + 1].toLong() and 0xFF shl 16) or
    (bytes[offset + 2].toLong() and 0xFF shl 8) or
    (bytes[offset + 3].toLong() and 0xFF)

private fun matchesAt(bytes: ByteArray, index: Int, terminator: ByteArray): Boolean {
    for (offset in terminator.indices) {
        if (bytes[index + offset] != terminator[offset]) return false
    }
    return true
}
