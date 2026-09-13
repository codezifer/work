package de.carsten.android.muzzic.id3

import de.carsten.android.muzzic.id3.model.frame.BinaryDataFrame
import de.carsten.android.muzzic.id3.model.frame.FrameHeader
import de.carsten.android.muzzic.id3.model.frame.FrameId
import de.carsten.android.muzzic.id3.model.frame.Id3Frame
import de.carsten.android.muzzic.id3.model.frame.TextInformationFrame
import de.carsten.android.muzzic.id3.model.frame.UnknownFrame
import de.carsten.android.muzzic.id3.model.frame.binary.ApicFrame
import de.carsten.android.muzzic.id3.model.frame.binary.PictureType
import de.carsten.android.muzzic.id3.model.frame.text.Tit2Frame
import de.carsten.android.muzzic.id3.model.frame.url.WcomFrame
import de.carsten.android.muzzic.id3.model.tag.DataBytes
import de.carsten.android.muzzic.id3.model.tag.TextEncoding
import de.carsten.android.muzzic.id3.model.visitor.Id3FrameVisitor
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class VisitorDelegationTest {
    private val header = FrameHeader.V24(FrameId("TIT2"), 0)

    @Test
    fun `specific handler wins over group handler`() {
        val probe = object : Id3FrameVisitor<String> {
            override fun visitGeneric(frame: Id3Frame) = "generic"
            override fun visitTit2(frame: Tit2Frame) = "specific"
        }
        val frame = Tit2Frame(header, TextEncoding.ISO_8859_1, listOf("Song"))
        assertThat(frame.accept(probe)).isEqualTo("specific")
    }

    @Test
    fun `group handler catches all text frames`() {
        val probe = object : Id3FrameVisitor<String> {
            override fun visitGeneric(frame: Id3Frame) = "generic"
            override fun visitText(frame: TextInformationFrame) = "text"
        }
        val frame = Tit2Frame(header, TextEncoding.ISO_8859_1, listOf("Song"))
        assertThat(frame.accept(probe)).isEqualTo("text")
    }

    @Test
    fun `generic fallback handles urls and binaries`() {
        val probe = object : Id3FrameVisitor<String> {
            override fun visitGeneric(frame: Id3Frame) = "generic"
        }
        assertThat(WcomFrame(FrameHeader.V24(FrameId("WCOM"), 0), "https://example.com").accept(probe))
            .isEqualTo("generic")
        assertThat(
            ApicFrame(
                FrameHeader.V24(FrameId("APIC"), 0),
                TextEncoding.ISO_8859_1,
                "image/jpeg",
                PictureType.COVER_FRONT,
                "",
                DataBytes.EMPTY,
            ).accept(probe),
        ).isEqualTo("generic")
    }

    @Test
    fun `unknown frames reach visitUnknown`() {
        var visited = false
        val probe = object : Id3FrameVisitor<Unit> {
            override fun visitGeneric(frame: Id3Frame) = Unit
            override fun visitUnknown(frame: UnknownFrame) {
                visited = true
            }
        }
        UnknownFrame(FrameHeader.V24(FrameId("XABC"), 3), DataBytes(byteArrayOf(1, 2, 3))).accept(probe)
        assertThat(visited).isTrue()
    }

    @Test
    fun `binary group handler catches apic`() {
        val probe = object : Id3FrameVisitor<String> {
            override fun visitGeneric(frame: Id3Frame) = "generic"
            override fun visitBinary(frame: BinaryDataFrame) = "binary"
        }
        val frame = ApicFrame(
            FrameHeader.V24(FrameId("APIC"), 0),
            TextEncoding.ISO_8859_1,
            "image/jpeg",
            PictureType.COVER_FRONT,
            "",
            DataBytes.EMPTY,
        )
        assertThat(frame.accept(probe)).isEqualTo("binary")
    }
}
