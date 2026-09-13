package de.carsten.android.muzzic.id3

import de.carsten.android.muzzic.id3.exceptions.Id3Exception
import de.carsten.android.muzzic.id3.model.frame.FrameFormatFlags
import de.carsten.android.muzzic.id3.model.frame.FrameId
import de.carsten.android.muzzic.id3.model.frame.FrameStatusFlags
import de.carsten.android.muzzic.id3.model.tag.Id3Version
import de.carsten.android.muzzic.id3.model.tag.TagHeaderFlags
import de.carsten.android.muzzic.id3.model.tag.TextEncoding
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test

class ModelPrimitivesTest {
    @Test
    fun `text encodings resolve by code`() {
        assertThat(TextEncoding.fromCode(0x00)).isEqualTo(TextEncoding.ISO_8859_1)
        assertThat(TextEncoding.fromCode(0x03)).isEqualTo(TextEncoding.UTF_8)
        assertThat(TextEncoding.fromCode(0x07)).isNull()
    }

    @Test
    fun `v23 supports only latin1 and utf16`() {
        assertThat(TextEncoding.ISO_8859_1.isSupportedBy(Id3Version.V2_3)).isTrue()
        assertThat(TextEncoding.UTF_16.isSupportedBy(Id3Version.V2_3)).isTrue()
        assertThat(TextEncoding.UTF_16BE.isSupportedBy(Id3Version.V2_3)).isFalse()
        assertThat(TextEncoding.UTF_8.isSupportedBy(Id3Version.V2_3)).isFalse()
        assertThat(TextEncoding.UTF_8.isSupportedBy(Id3Version.V2_4)).isTrue()
    }

    @Test
    fun `frame ids validate charset and length`() {
        assertThat(FrameId("TIT2").value).isEqualTo("TIT2")
        assertThat(FrameId("LINK").value).isEqualTo("LINK")
        assertThatThrownBy { FrameId("tit2") }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { FrameId("TOOLONG") }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `frame id parse detects padding`() {
        assertThatThrownBy { FrameId.parse(byteArrayOf(0, 0, 0, 0)) }.isInstanceOf(Id3Exception::class.java)
    }

    @Test
    fun `tag header flags roundtrip`() {
        val flags = TagHeaderFlags(unsynchronisation = true, extendedHeader = true, footerPresent = true)
        val parsed = TagHeaderFlags.fromByte(Id3Version.V2_4, flags.toByte(Id3Version.V2_4))
        assertThat(parsed).isEqualTo(flags)
    }

    @Test
    fun `tag header flags ignore footer in v23`() {
        val parsed = TagHeaderFlags.fromByte(Id3Version.V2_3, 0x10.toByte())
        assertThat(parsed.footerPresent).isFalse()
    }

    @Test
    fun `frame status flags differ per version`() {
        assertThat(FrameStatusFlags.fromByte(Id3Version.V2_3, 0xA0.toByte()))
            .isEqualTo(FrameStatusFlags(tagAlterDiscard = true, fileAlterDiscard = false, readOnly = true))
        assertThat(FrameStatusFlags.fromByte(Id3Version.V2_4, 0x50.toByte()))
            .isEqualTo(FrameStatusFlags(tagAlterDiscard = true, fileAlterDiscard = false, readOnly = true))
    }

    @Test
    fun `frame format flags parse`() {
        assertThat(FrameFormatFlags.V23.fromByte(0xE0.toByte()))
            .isEqualTo(FrameFormatFlags.V23(compression = true, encryption = true, grouping = true))
        assertThat(FrameFormatFlags.V24.fromByte(0x4F.toByte()))
            .isEqualTo(
                FrameFormatFlags.V24(
                    grouping = true,
                    compression = true,
                    encryption = true,
                    unsynchronisation = true,
                    dataLengthIndicator = true,
                ),
            )
    }
}
