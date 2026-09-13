package de.carsten.android.muzzic.id3

import de.carsten.android.muzzic.id3.mapper.Id3MetadataMapper
import de.carsten.android.muzzic.id3.model.frame.FrameHeader
import de.carsten.android.muzzic.id3.model.frame.FrameId
import de.carsten.android.muzzic.id3.model.frame.UnknownFrame
import de.carsten.android.muzzic.id3.model.frame.text.Tit2Frame
import de.carsten.android.muzzic.id3.model.tag.SynchsafeInt
import de.carsten.android.muzzic.id3.parser.FrameDecoderRegistry
import de.carsten.android.muzzic.id3.parser.Id3Parser
import java.io.ByteArrayInputStream
import org.assertj.core.api.Assertions.assertThat
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
}
