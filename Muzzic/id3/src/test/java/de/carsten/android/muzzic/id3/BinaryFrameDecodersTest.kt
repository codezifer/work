package de.carsten.android.muzzic.id3

import de.carsten.android.muzzic.id3.model.frame.FrameHeader
import de.carsten.android.muzzic.id3.model.frame.FrameId
import de.carsten.android.muzzic.id3.model.frame.UnknownFrame
import de.carsten.android.muzzic.id3.model.frame.binary.ApicFrame
import de.carsten.android.muzzic.id3.model.frame.binary.AspiFrame
import de.carsten.android.muzzic.id3.model.frame.binary.ComrFrame
import de.carsten.android.muzzic.id3.model.frame.binary.Equ2Frame
import de.carsten.android.muzzic.id3.model.frame.binary.Equ2Interpolation
import de.carsten.android.muzzic.id3.model.frame.binary.EtcoFrame
import de.carsten.android.muzzic.id3.model.frame.binary.EventType
import de.carsten.android.muzzic.id3.model.frame.binary.GeobFrame
import de.carsten.android.muzzic.id3.model.frame.binary.OwneFrame
import de.carsten.android.muzzic.id3.model.frame.binary.PictureType
import de.carsten.android.muzzic.id3.model.frame.binary.ReceivedAs
import de.carsten.android.muzzic.id3.model.frame.binary.Rva2ChannelType
import de.carsten.android.muzzic.id3.model.frame.binary.Rva2Frame
import de.carsten.android.muzzic.id3.model.frame.language.SyltContentType
import de.carsten.android.muzzic.id3.model.frame.language.SyltFrame
import de.carsten.android.muzzic.id3.model.frame.language.UserFrame
import de.carsten.android.muzzic.id3.model.tag.LanguageCode
import de.carsten.android.muzzic.id3.model.tag.TextEncoding
import de.carsten.android.muzzic.id3.model.tag.TimestampFormat
import de.carsten.android.muzzic.id3.parser.FrameDecoderRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class BinaryFrameDecodersTest {
    private fun header(id: String, size: Int) = FrameHeader.V24(FrameId(id), size)

    private fun decode(id: String, body: ByteArray, offset: Int = 20) = FrameDecoderRegistry.decode(header(id, body.size), body, offset)

    @Test
    fun `decodes apic with picture offset`() {
        val body = byteArrayOf(0x00) + "image/jpeg".toByteArray() + byteArrayOf(0x00, 0x03) +
            "Cover".toByteArray() + byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04)
        val frame = decode("APIC", body) as ApicFrame
        assertThat(frame.mimeType).isEqualTo("image/jpeg")
        assertThat(frame.pictureType).isEqualTo(PictureType.COVER_FRONT)
        assertThat(frame.description).isEqualTo("Cover")
        assertThat(frame.pictureData.bytes).isEqualTo(byteArrayOf(0x01, 0x02, 0x03, 0x04))
        assertThat(frame.pictureData.offset).isEqualTo(20 + 19)
        assertThat(frame.pictureData.length).isEqualTo(4)
    }

    @Test
    fun `decodes apic link with url payload`() {
        val body = byteArrayOf(0x00) + "-->".toByteArray() + byteArrayOf(0x00, 0x03, 0x00) +
            "https://example.com/cover.jpg".toByteArray()
        val frame = decode("APIC", body) as ApicFrame
        assertThat(frame.mimeType).isEqualTo("-->")
        assertThat(frame.pictureData.bytes.decodeToString()).isEqualTo("https://example.com/cover.jpg")
    }

    @Test
    fun `decodes truncated apic as unknown`() {
        val frame = decode("APIC", byteArrayOf(0x00, 'x'.code.toByte()))
        assertThat(frame).isInstanceOf(UnknownFrame::class.java)
    }

    @Test
    fun `decodes geob`() {
        val body = byteArrayOf(0x00) + "application/octet-stream".toByteArray() + byteArrayOf(0x00) +
            "file.bin".toByteArray() + byteArrayOf(0x00) + "desc".toByteArray() + byteArrayOf(0x00, 0x07)
        val frame = decode("GEOB", body) as GeobFrame
        assertThat(frame.mimeType).isEqualTo("application/octet-stream")
        assertThat(frame.filename).isEqualTo("file.bin")
        assertThat(frame.description).isEqualTo("desc")
        assertThat(frame.data.bytes).isEqualTo(byteArrayOf(0x07))
    }

    @Test
    fun `decodes user terms`() {
        val body = byteArrayOf(0x00) + "eng".toByteArray() + "Some terms".toByteArray()
        val frame = decode("USER", body) as UserFrame
        assertThat(frame.language).isEqualTo(LanguageCode("eng"))
        assertThat(frame.text).isEqualTo("Some terms")
    }

    @Test
    fun `decodes owne with fixed date`() {
        val body = byteArrayOf(0x00) + "EUR12.50".toByteArray() + byteArrayOf(0x00) +
            "20230115".toByteArray() + "Seller".toByteArray()
        val frame = decode("OWNE", body) as OwneFrame
        assertThat(frame.pricePaid).isEqualTo("EUR12.50")
        assertThat(frame.purchaseDate).isEqualTo("20230115")
        assertThat(frame.seller).isEqualTo("Seller")
    }

    @Test
    fun `decodes sylt entries`() {
        val body = byteArrayOf(0x00) + "eng".toByteArray() + byteArrayOf(0x02, 0x01) +
            "desc".toByteArray() + byteArrayOf(0x00) +
            "Hello".toByteArray() + byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x64.toByte()) +
            "World".toByteArray() + byteArrayOf(0x00, 0x00, 0x00, 0x00, 0xC8.toByte())
        val frame = decode("SYLT", body) as SyltFrame
        assertThat(frame.format).isEqualTo(TimestampFormat.MILLISECONDS)
        assertThat(frame.contentType).isEqualTo(SyltContentType.LYRICS)
        assertThat(frame.entries.map { it.text }).containsExactly("Hello", "World")
        assertThat(frame.entries.map { it.timestamp }).containsExactly(100L, 200L)
    }

    @Test
    fun `stops sylt at truncated tail`() {
        val body = byteArrayOf(0x00) + "eng".toByteArray() + byteArrayOf(0x02, 0x01, 0x00) +
            "A".toByteArray() + byteArrayOf(0x00, 0x01, 0x02)
        val frame = decode("SYLT", body) as SyltFrame
        assertThat(frame.entries).isEmpty()
    }

    @Test
    fun `decodes etco skipping padding`() {
        val body = byteArrayOf(0x02, 0x01, 0x00, 0x00, 0x00, 0x32, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x64)
        val frame = decode("ETCO", body) as EtcoFrame
        assertThat(frame.format).isEqualTo(TimestampFormat.MILLISECONDS)
        assertThat(frame.events.map { it.type })
            .containsExactly(EventType.END_OF_INITIAL_SILENCE, EventType.MAIN_PART_START)
        assertThat(frame.events.map { it.timestamp }).containsExactly(50L, 100L)
    }

    @Test
    fun `resolves etco escape byte`() {
        val body = byteArrayOf(0x02, 0xFF.toByte(), 0x03, 0x00, 0x00, 0x00, 0x07)
        val frame = decode("ETCO", body) as EtcoFrame
        assertThat(frame.events.map { it.type }).containsExactly(EventType.MAIN_PART_START)
    }

    @Test
    fun `falls back on unknown etco code`() {
        val frame = decode("ETCO", byteArrayOf(0x02, 0x17, 0x00, 0x00, 0x00, 0x01))
        assertThat(frame).isInstanceOf(UnknownFrame::class.java)
    }

    @Test
    fun `decodes rva2 channels with peak`() {
        val body = "track".toByteArray() + byteArrayOf(0x00, 0x01, 0x04, 0x00, 0x00, 0x03, 0xFC.toByte(), 0x00, 0x10, 0x0A, 0x0B)
        val frame = decode("RVA2", body) as Rva2Frame
        assertThat(frame.identification).isEqualTo("track")
        assertThat(frame.channels).hasSize(2)
        assertThat(frame.channels[0].channelType).isEqualTo(Rva2ChannelType.MASTER_VOLUME)
        assertThat(frame.channels[0].volumeAdjustment).isEqualTo(0x0400.toShort())
        assertThat(frame.channels[0].peakVolume).isNull()
        assertThat(frame.channels[1].channelType).isEqualTo(Rva2ChannelType.FRONT_LEFT)
        assertThat(frame.channels[1].peakVolume?.bytes).isEqualTo(byteArrayOf(0x0A, 0x0B))
    }

    @Test
    fun `decodes equ2 points`() {
        val body = byteArrayOf(0x01) + "id".toByteArray() + byteArrayOf(0x00, 0x03, 0xE8.toByte(), 0x04, 0x00)
        val frame = decode("EQU2", body) as Equ2Frame
        assertThat(frame.interpolation).isEqualTo(Equ2Interpolation.LINEAR)
        assertThat(frame.points.map { it.frequency }).containsExactly(1000)
        assertThat(frame.points.map { it.volumeAdjustment }).containsExactly(0x0400.toShort())
    }

    @Test
    fun `decodes comr without logo`() {
        val body = byteArrayOf(0x00) + "EUR9.99".toByteArray() + byteArrayOf(0x00) +
            "20241231".toByteArray() + "shop.example".toByteArray() + byteArrayOf(0x00, 0x03) +
            "Seller".toByteArray() + byteArrayOf(0x00) + "Desc".toByteArray() + byteArrayOf(0x00)
        val frame = decode("COMR", body) as ComrFrame
        assertThat(frame.priceString).isEqualTo("EUR9.99")
        assertThat(frame.validUntil).isEqualTo("20241231")
        assertThat(frame.receivedAs).isEqualTo(ReceivedAs.FILE_OVER_INTERNET)
        assertThat(frame.mimeType).isNull()
        assertThat(frame.sellerLogo).isNull()
    }

    @Test
    fun `decodes comr with logo offset`() {
        val body = byteArrayOf(0x00) + "EUR9.99".toByteArray() + byteArrayOf(0x00) +
            "20241231".toByteArray() + "shop.example".toByteArray() + byteArrayOf(0x00, 0x03) +
            "Seller".toByteArray() + byteArrayOf(0x00) + "Desc".toByteArray() + byteArrayOf(0x00) +
            "image/png".toByteArray() + byteArrayOf(0x00, 0x07, 0x08)
        val frame = decode("COMR", body, 100) as ComrFrame
        assertThat(frame.mimeType).isEqualTo("image/png")
        assertThat(frame.sellerLogo?.bytes).isEqualTo(byteArrayOf(0x07, 0x08))
        assertThat(frame.sellerLogo?.offset).isEqualTo(100 + body.size - 2)
    }

    @Test
    fun `decodes aspi with eight bit fractions`() {
        val body = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, 0xE8.toByte(), 0x00, 0x02, 0x08, 0x0A, 0xC8.toByte())
        val frame = decode("ASPI", body) as AspiFrame
        assertThat(frame.indexedDataStart).isEqualTo(0u)
        assertThat(frame.indexedDataLength).isEqualTo(1000u)
        assertThat(frame.fractions).containsExactly(10, 200)
        assertThat(frame.bitsPerPoint).isEqualTo(8u.toUByte())
    }

    @Test
    fun `decodes aspi with sixteen bit fractions`() {
        val body = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, 0xE8.toByte(), 0x00, 0x01, 0x10, 0x01, 0x00)
        val frame = decode("ASPI", body) as AspiFrame
        assertThat(frame.fractions).containsExactly(256)
    }

    @Test
    fun `rejects aspi with unsupported precision`() {
        val body = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, 0xE8.toByte(), 0x00, 0x01, 0x0C, 0x01)
        assertThat(decode("ASPI", body)).isInstanceOf(UnknownFrame::class.java)
    }

    @Test
    fun `strips bom from utf16 text`() {
        val body = byteArrayOf(0x01, 0xFF.toByte(), 0xFE.toByte(), 'H'.code.toByte(), 0x00, 'i'.code.toByte(), 0x00, 0x00, 0x00)
        val frame = decode("TIT2", body) as de.carsten.android.muzzic.id3.model.frame.text.Tit2Frame
        assertThat(frame.values).containsExactly("Hi")
    }

    @Test
    fun `decodes latin1 url bytes`() {
        val body = "café".toByteArray(Charsets.ISO_8859_1)
        val frame = decode("WCOM", body) as de.carsten.android.muzzic.id3.model.frame.url.WcomFrame
        assertThat(frame.url).isEqualTo("café")
    }

    @Test
    fun `text encoding resolves`() {
        assertThat(TextEncoding.ISO_8859_1.code).isEqualTo(0x00.toByte())
    }
}
