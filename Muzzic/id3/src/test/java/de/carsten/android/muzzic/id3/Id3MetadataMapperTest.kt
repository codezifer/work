package de.carsten.android.muzzic.id3

import de.carsten.android.muzzic.id3.mapper.Id3MetadataMapper
import de.carsten.android.muzzic.id3.model.frame.FrameHeader
import de.carsten.android.muzzic.id3.model.frame.FrameId
import de.carsten.android.muzzic.id3.model.frame.binary.ApicFrame
import de.carsten.android.muzzic.id3.model.frame.binary.PcntFrame
import de.carsten.android.muzzic.id3.model.frame.binary.PictureType
import de.carsten.android.muzzic.id3.model.frame.binary.PopmFrame
import de.carsten.android.muzzic.id3.model.frame.language.CommFrame
import de.carsten.android.muzzic.id3.model.frame.text.TalbFrame
import de.carsten.android.muzzic.id3.model.frame.text.TconFrame
import de.carsten.android.muzzic.id3.model.frame.text.TdrcFrame
import de.carsten.android.muzzic.id3.model.frame.text.Tit2Frame
import de.carsten.android.muzzic.id3.model.frame.text.TlenFrame
import de.carsten.android.muzzic.id3.model.frame.text.Tpe1Frame
import de.carsten.android.muzzic.id3.model.frame.text.TrckFrame
import de.carsten.android.muzzic.id3.model.tag.DataBytes
import de.carsten.android.muzzic.id3.model.tag.LanguageCode
import de.carsten.android.muzzic.id3.model.tag.TextEncoding
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class Id3MetadataMapperTest {
    private fun header(id: String) = FrameHeader.V24(FrameId(id), 0)

    @Test
    fun `maps common frames to metadata`() {
        val metadata = Id3MetadataMapper.map(
            listOf(
                Tit2Frame(header("TIT2"), TextEncoding.ISO_8859_1, listOf("Song")),
                Tpe1Frame(header("TPE1"), TextEncoding.ISO_8859_1, listOf("Artist")),
                TalbFrame(header("TALB"), TextEncoding.ISO_8859_1, listOf("Album")),
                TrckFrame(header("TRCK"), TextEncoding.ISO_8859_1, listOf("4/9")),
                TdrcFrame(header("TDRC"), TextEncoding.ISO_8859_1, listOf("2023-05-01")),
                PopmFrame(header("POPM"), "user@example.com", 200u, null),
                ApicFrame(
                    header("APIC"),
                    TextEncoding.ISO_8859_1,
                    "image/jpeg",
                    PictureType.COVER_FRONT,
                    "",
                    DataBytes(byteArrayOf(1, 2, 3)),
                ),
            ),
        )
        assertThat(metadata.title).isEqualTo("Song")
        assertThat(metadata.artist).isEqualTo("Artist")
        assertThat(metadata.album).isEqualTo("Album")
        assertThat(metadata.track).isEqualTo(4)
        assertThat(metadata.totalTracks).isEqualTo(9)
        assertThat(metadata.albumYear).isEqualTo(2023)
        assertThat(metadata.rating).isEqualTo(200)
        assertThat(metadata.albumArt?.mimeType).isEqualTo("image/jpeg")
        assertThat(metadata.albumArt?.length).isEqualTo(3)
    }

    @Test
    fun `first frame wins and empty list yields blanks`() {
        val metadata = Id3MetadataMapper.map(
            listOf(
                Tit2Frame(header("TIT2"), TextEncoding.ISO_8859_1, listOf("First")),
                Tit2Frame(header("TIT2"), TextEncoding.ISO_8859_1, listOf("Second")),
            ),
        )
        assertThat(metadata.title).isEqualTo("First")
        assertThat(Id3MetadataMapper.map(emptyList()).title).isNull()
    }

    @Test
    fun `maps genre duration comment and play count`() {
        val metadata = Id3MetadataMapper.map(
            listOf(
                TconFrame(header("TCON"), TextEncoding.ISO_8859_1, listOf("Rock")),
                TlenFrame(header("TLEN"), TextEncoding.ISO_8859_1, listOf("187000")),
                CommFrame(header("COMM"), TextEncoding.ISO_8859_1, LanguageCode("eng"), "", "Nice song"),
                PcntFrame(header("PCNT"), DataBytes(byteArrayOf(0x00, 0x00, 0x00, 0x2A))),
            ),
        )
        assertThat(metadata.genre).isEqualTo("Rock")
        assertThat(metadata.duration).isEqualTo(187000L)
        assertThat(metadata.comment).isEqualTo("Nice song")
        assertThat(metadata.playCount).isEqualTo(42)
    }

    @Test
    fun `maps popm counter to play count when pcnt is absent`() {
        val metadata = Id3MetadataMapper.map(
            listOf(
                PopmFrame(header("POPM"), "user@example.com", 200u, DataBytes(byteArrayOf(0x00, 0x2A))),
            ),
        )
        assertThat(metadata.rating).isEqualTo(200)
        assertThat(metadata.playCount).isEqualTo(42)
    }

    @Test
    fun `prefers pcnt over popm counter for play count`() {
        val metadata = Id3MetadataMapper.map(
            listOf(
                PcntFrame(header("PCNT"), DataBytes(byteArrayOf(0x00, 0x00, 0x00, 0x07))),
                PopmFrame(header("POPM"), "user@example.com", 100u, DataBytes(byteArrayOf(0x00, 0x2A))),
            ),
        )
        assertThat(metadata.playCount).isEqualTo(7)
    }

    @Test
    fun `coerces overflowing play count and ignores invalid duration`() {
        val metadata = Id3MetadataMapper.map(
            listOf(
                TlenFrame(header("TLEN"), TextEncoding.ISO_8859_1, listOf("not-a-number")),
                PcntFrame(header("PCNT"), DataBytes(byteArrayOf(0x01, 0x00, 0x00, 0x00, 0x00))),
            ),
        )
        assertThat(metadata.duration).isNull()
        assertThat(metadata.playCount).isEqualTo(Int.MAX_VALUE)
    }

    @Test
    fun `maps album art stream position`() {
        val metadata = Id3MetadataMapper.map(
            listOf(
                ApicFrame(
                    header("APIC"),
                    TextEncoding.ISO_8859_1,
                    "image/png",
                    PictureType.COVER_FRONT,
                    "",
                    DataBytes(byteArrayOf(1, 2), offset = 50),
                ),
            ),
        )
        assertThat(metadata.albumArt?.offset).isEqualTo(50)
        assertThat(metadata.albumArt?.length).isEqualTo(2)
    }
}
