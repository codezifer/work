package de.carsten.android.muzzic.id3

import de.carsten.android.muzzic.id3.exceptions.Id3Exception
import de.carsten.android.muzzic.id3.mapper.Id3MetadataMapper
import de.carsten.android.muzzic.id3.model.frame.FrameHeader
import de.carsten.android.muzzic.id3.model.frame.FrameId
import de.carsten.android.muzzic.id3.model.frame.UnknownFrame
import de.carsten.android.muzzic.id3.model.frame.binary.ApicFrame
import de.carsten.android.muzzic.id3.model.frame.text.Tit2Frame
import de.carsten.android.muzzic.id3.model.tag.SynchsafeInt
import de.carsten.android.muzzic.id3.model.tag.TextEncoding
import de.carsten.android.muzzic.id3.parser.FrameDecoderRegistry
import de.carsten.android.muzzic.id3.parser.Id3ParseOptions
import de.carsten.android.muzzic.id3.parser.Id3Parser
import java.io.ByteArrayInputStream
import java.nio.charset.Charset
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test

class Id3ParserTest {
    @Test
    fun `reverse unsynchronisation removes inserted zeros`() {
        val reversed = Id3Parser.reverseUnsynchronisation(byteArrayOf(0xFF.toByte(), 0x00, 0xE0.toByte(), 0x01))
        assertThat(reversed).isEqualTo(byteArrayOf(0xFF.toByte(), 0xE0.toByte(), 0x01))
    }

    @Test
    fun `registry decodes text frames and falls back to unknown`() {
        val header = FrameHeader.V24(FrameId("TIT2"), 6)
        val frame = FrameDecoderRegistry.decode(header, byteArrayOf(0x00, 'H'.code.toByte(), 'i'.code.toByte(), 0x00))
        assertThat(frame).isInstanceOf(Tit2Frame::class.java)
        assertThat((frame as Tit2Frame).values).containsExactly("Hi")

        val unknown = FrameDecoderRegistry.decode(FrameHeader.V24(FrameId("XABC"), 2), byteArrayOf(1, 2))
        assertThat(unknown).isInstanceOf(UnknownFrame::class.java)
    }

    @Test
    fun `registry fallback keeps explicit body offset`() {
        val unknown = FrameDecoderRegistry.decode(
            FrameHeader.V24(FrameId("XABC"), 2),
            byteArrayOf(1, 2),
            20,
        ) as UnknownFrame
        assertThat(unknown.raw.offset).isEqualTo(20)
        assertThat(unknown.raw.length).isEqualTo(2)
    }

    @Test
    fun `parses minimal v24 tag with title frame`() {
        val body = byteArrayOf(0x00) + "Title".toByteArray() + byteArrayOf(0x00)
        val frameHeader = "TIT2".toByteArray() + SynchsafeInt.encode(body.size) + byteArrayOf(0x00, 0x00)
        val tagBody = frameHeader + body
        val header = "ID3".toByteArray() + byteArrayOf(0x04, 0x00, 0x00) + SynchsafeInt.encode(tagBody.size)
        val tag = Id3Parser.parse(ByteArrayInputStream(header + tagBody))
        assertThat(tag.frames).hasSize(1)
        val title = tag.frames.first() as Tit2Frame
        assertThat(title.values).containsExactly("Title")
        assertThat(tag.paddingSize).isEqualTo(0)
    }

    @Test
    fun `parsed bodies carry absolute stream offsets`() {
        val frameHeader = "XABC".toByteArray() + SynchsafeInt.encode(2) + byteArrayOf(0x00, 0x00)
        val tagBody = frameHeader + byteArrayOf(0x01, 0x02)
        val header = "ID3".toByteArray() + byteArrayOf(0x04, 0x00, 0x00) + SynchsafeInt.encode(tagBody.size)
        val tag = Id3Parser.parse(ByteArrayInputStream(header + tagBody))
        val unknown = tag.frames.first() as UnknownFrame
        assertThat(unknown.raw.offset).isEqualTo(20)
        assertThat(unknown.raw.length).isEqualTo(2)
    }

    @Test
    fun `v23 tag unsynchronisation reverses frame headers once`() {
        // Plain v2.3 frame: status byte $FF followed by format $00. The
        // unsynchronised on-disk form inserts an extra $00 after $FF, so a
        // parser reversing per frame body only would misalign the header.
        val plainBody = byteArrayOf(0x00, 'H'.code.toByte(), 'i'.code.toByte())
        val plainFrame = "TIT2".toByteArray() + u32(plainBody.size) + byteArrayOf(0xFF.toByte(), 0x00) + plainBody
        val onDiskBody = unsynchronise(plainFrame)
        assertThat(onDiskBody.size).isEqualTo(plainFrame.size + 1)
        val header = "ID3".toByteArray() + byteArrayOf(0x03, 0x00, 0x80.toByte()) + SynchsafeInt.encode(onDiskBody.size)
        val tag = Id3Parser.parse(ByteArrayInputStream(header + onDiskBody))
        assertThat(tag.frames).hasSize(1)
        assertThat((tag.frames.first() as Tit2Frame).values).containsExactly("Hi")
    }

    @Test
    fun `v24 frame unsynchronisation still reverses per body`() {
        val plainBody = byteArrayOf(0x00, 0xFF.toByte(), 0xE0.toByte())
        val onDiskBody = unsynchronise(plainBody)
        val frameHeader = "TIT2".toByteArray() + SynchsafeInt.encode(onDiskBody.size) + byteArrayOf(0x00, 0x02)
        val tagBody = frameHeader + onDiskBody
        val header = "ID3".toByteArray() + byteArrayOf(0x04, 0x00, 0x00) + SynchsafeInt.encode(tagBody.size)
        val tag = Id3Parser.parse(ByteArrayInputStream(header + tagBody))
        val title = tag.frames.first() as Tit2Frame
        val expected = String(byteArrayOf(0xFF.toByte(), 0xE0.toByte()), Charset.forName("ISO-8859-1"))
        assertThat(title.values).containsExactly(expected)
    }

    @Test
    fun `skipped apic keeps position without picture bytes`() {
        val picture = byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte())
        val body = byteArrayOf(0x00) + "image/jpeg".toByteArray() + byteArrayOf(0x00, 0x03) +
            "Cover".toByteArray() + byteArrayOf(0x00) + picture
        val frameHeader = "APIC".toByteArray() + SynchsafeInt.encode(body.size) + byteArrayOf(0x00, 0x00)
        val tagBody = frameHeader + body
        val header = "ID3".toByteArray() + byteArrayOf(0x04, 0x00, 0x00) + SynchsafeInt.encode(tagBody.size)
        val tag = Id3Parser.parse(ByteArrayInputStream(header + tagBody), Id3ParseOptions(skipPictureData = true))
        val apic = tag.frames.first() as ApicFrame
        assertThat(apic.mimeType).isEqualTo("image/jpeg")
        assertThat(apic.description).isEqualTo("Cover")
        assertThat(apic.pictureData.bytes).isEmpty()
        assertThat(apic.pictureData.offset).isEqualTo(39)
        assertThat(apic.pictureData.length).isEqualTo(3)
        val art = Id3MetadataMapper.map(tag.frames).albumArt
        assertThat(art?.offset).isEqualTo(39)
        assertThat(art?.length).isEqualTo(3)
        assertThat(art?.isValid()).isTrue()
    }

    @Test
    fun `compressed frames are preserved as unknown`() {
        val body = byteArrayOf(0x00) + "Hi".toByteArray()
        // v2.4 frame with compression bit set in the format flags.
        val frameHeader = "TIT2".toByteArray() + SynchsafeInt.encode(body.size) + byteArrayOf(0x00, 0x08)
        val tagBody = frameHeader + body
        val header = "ID3".toByteArray() + byteArrayOf(0x04, 0x00, 0x00) + SynchsafeInt.encode(tagBody.size)
        val tag = Id3Parser.parse(ByteArrayInputStream(header + tagBody))
        val unknown = tag.frames.first() as UnknownFrame
        assertThat(unknown.raw.bytes).isEqualTo(body)
    }

    @Test
    fun `malformed input fails with id3 exception`() {
        assertThatThrownBy {
            Id3Parser.parse(ByteArrayInputStream("ID3".toByteArray() + byteArrayOf(0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)))
        }.isInstanceOf(Id3Exception::class.java)

        assertThatThrownBy {
            Id3Parser.parse(ByteArrayInputStream("MP3".toByteArray() + ByteArray(7)))
        }.isInstanceOf(Id3Exception::class.java)

        assertThatThrownBy {
            Id3Parser.parse(ByteArrayInputStream(ByteArray(0)))
        }.isInstanceOf(Id3Exception::class.java)
    }

    @Test
    fun `text encodings cache their charset`() {
        TextEncoding.entries.forEach { encoding ->
            assertThat(encoding.charset.name()).isEqualTo(encoding.charsetName)
            assertThat(encoding.charset).isSameAs(Charset.forName(encoding.charsetName))
        }
    }

    @Test
    fun `padding check precedes frame id parsing`() {
        assertThat(FrameId.isPadding(byteArrayOf(0, 0, 0, 0))).isTrue()
        assertThat(FrameId.isPadding("TIT2".toByteArray())).isFalse()
        assertThatThrownBy { FrameId.parse(byteArrayOf(0, 0, 0, 0)) }.isInstanceOf(Id3Exception::class.java)
    }

    @Test
    fun `maps apic picture bytes to absolute stream offset`() {
        val picture = byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte())
        val body = byteArrayOf(0x00) + "image/jpeg".toByteArray() + byteArrayOf(0x00, 0x03) +
            "Cover".toByteArray() + byteArrayOf(0x00) + picture
        val frameHeader = "APIC".toByteArray() + SynchsafeInt.encode(body.size) + byteArrayOf(0x00, 0x00)
        val tagBody = frameHeader + body
        val header = "ID3".toByteArray() + byteArrayOf(0x04, 0x00, 0x00) + SynchsafeInt.encode(tagBody.size)
        val tag = Id3Parser.parse(ByteArrayInputStream(header + tagBody))
        val art = Id3MetadataMapper.map(tag.frames).albumArt
        assertThat(art).isNotNull()
        // 10 tag header + 10 frame header + 1 encoding + 10 mime + 1 term + 1 type + 5 desc + 1 term.
        assertThat(art?.offset).isEqualTo(39)
        assertThat(art?.length).isEqualTo(3)
        assertThat(art?.mimeType).isEqualTo("image/jpeg")
        assertThat(art?.hashCode).isEqualTo(picture.contentHashCode())
        assertThat(art?.isValid()).isTrue()
    }

    private fun u32(value: Int): ByteArray = byteArrayOf(
        (value shr 24 and 0xFF).toByte(),
        (value shr 16 and 0xFF).toByte(),
        (value shr 8 and 0xFF).toByte(),
        (value and 0xFF).toByte(),
    )

    private fun unsynchronise(bytes: ByteArray): ByteArray {
        val out = mutableListOf<Byte>()
        bytes.forEach {
            out.add(it)
            if (it == 0xFF.toByte()) out.add(0x00)
        }
        return out.toByteArray()
    }
}
