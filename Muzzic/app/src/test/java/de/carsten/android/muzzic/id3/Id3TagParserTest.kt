package de.carsten.android.muzzic.id3

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.id3.ID3v23Frame
import org.jaudiotagger.tag.id3.framebody.FrameBodyPCNT
import org.jaudiotagger.tag.id3.framebody.FrameBodyPOPM
import org.junit.Test

class Id3TagParserTest {

    @Test
    fun `parseTrackString parses track and total with various separators`() {
        assertThat(Id3TagParser.parseTrackString("1/10")).isEqualTo(Pair(1, 10))
        assertThat(Id3TagParser.parseTrackString("01/10")).isEqualTo(Pair(1, 10))
        assertThat(Id3TagParser.parseTrackString("2\\12")).isEqualTo(Pair(2, 12))
        assertThat(Id3TagParser.parseTrackString("3-15")).isEqualTo(Pair(3, 15))
        assertThat(Id3TagParser.parseTrackString("4:20")).isEqualTo(Pair(4, 20))
        assertThat(Id3TagParser.parseTrackString("5")).isEqualTo(Pair(5, -1))
        assertThat(Id3TagParser.parseTrackString("")).isEqualTo(Pair(-1, -1))
        assertThat(Id3TagParser.parseTrackString(null)).isEqualTo(Pair(-1, -1))
    }

    @Test
    fun `extractMetadata extracts rating and play count from POPM robustly`() {
        val tag = mockk<Tag>()
        val popmFrame = mockk<ID3v23Frame>()
        val popmBody = mockk<FrameBodyPOPM>()

        every { tag.getFirst(FieldKey.TITLE) } returns "Title"
        every { tag.getFirst(FieldKey.ALBUM) } returns "Album"
        every { tag.getFirst(FieldKey.ARTIST) } returns "Artist"
        every { tag.getFirst(FieldKey.GENRE) } returns "Genre"
        every { tag.getFirst(FieldKey.YEAR) } returns "2023"
        every { tag.getFirst(FieldKey.ORIGINAL_YEAR) } returns ""
        every { tag.getFirst(FieldKey.TRACK) } returns "1/10"
        every { tag.getFirst(FieldKey.TRACK_TOTAL) } returns ""
        every { tag.getFirstField("POPM") } returns popmFrame
        every { popmFrame.body } returns popmBody
        every { popmBody.rating } returns 128L
        every { popmBody.counter } returns 42L

        // General FieldKey fallback mock
        every { tag.getFirst(FieldKey.RATING) } returns "0"

        val metadata = Id3TagParser.extractMetadata(
            mockk {
                every { this@mockk.tag } returns tag
                every { audioHeader } returns mockk {
                    every { preciseTrackLength } returns 0.0
                }
            },
        )

        assertThat(metadata.year).isEqualTo(2023)
        assertThat(metadata.rating).isEqualTo(128)
        assertThat(metadata.playCount).isEqualTo(42)
        assertThat(metadata.trackNumber).isEqualTo(1)
        assertThat(metadata.totalTracks).isEqualTo(10)
    }

    @Test
    fun `extractMetadata handles separate TRACK_TOTAL field`() {
        val tag = mockk<Tag>()

        every { tag.getFirst(FieldKey.TITLE) } returns "Title"
        every { tag.getFirst(FieldKey.ALBUM) } returns "Album"
        every { tag.getFirst(FieldKey.ARTIST) } returns "Artist"
        every { tag.getFirst(FieldKey.GENRE) } returns "Genre"
        every { tag.getFirst(FieldKey.YEAR) } returns "2020"
        every { tag.getFirst(FieldKey.ORIGINAL_YEAR) } returns ""
        every { tag.getFirst(FieldKey.TRACK) } returns "5"
        every { tag.getFirst(FieldKey.TRACK_TOTAL) } returns "20"
        every { tag.getFirstField("POPM") } returns null
        every { tag.getFirstField("PCNT") } returns null
        every { tag.getFirst(FieldKey.RATING) } returns "255"

        val metadata = Id3TagParser.extractMetadata(
            mockk {
                every { this@mockk.tag } returns tag
                every { audioHeader } returns mockk {
                    every { preciseTrackLength } returns 0.0
                }
            },
        )

        assertThat(metadata.rating).isEqualTo(255)
        assertThat(metadata.trackNumber).isEqualTo(5)
        assertThat(metadata.totalTracks).isEqualTo(20)
    }

    @Test
    fun `extractMetadata extracts play count from PCNT`() {
        val tag = mockk<Tag>()
        val pcntFrame = mockk<ID3v23Frame>()
        val pcntBody = mockk<FrameBodyPCNT>()

        every { tag.getFirst(FieldKey.TITLE) } returns "Title"
        every { tag.getFirst(FieldKey.ALBUM) } returns "Album"
        every { tag.getFirst(FieldKey.ARTIST) } returns "Artist"
        every { tag.getFirst(FieldKey.GENRE) } returns "Genre"
        every { tag.getFirst(FieldKey.YEAR) } returns ""
        every { tag.getFirst(FieldKey.ORIGINAL_YEAR) } returns ""
        every { tag.getFirst(FieldKey.TRACK) } returns ""
        every { tag.getFirst(FieldKey.TRACK_TOTAL) } returns ""
        every { tag.getFirstField("POPM") } returns null
        every { tag.getFirstField("PCNT") } returns pcntFrame
        every { pcntFrame.body } returns pcntBody
        every { pcntBody.counter } returns 100L
        every { tag.getFirst(FieldKey.RATING) } returns "0"

        val metadata = Id3TagParser.extractMetadata(
            mockk {
                every { this@mockk.tag } returns tag
                every { audioHeader } returns mockk {
                    every { preciseTrackLength } returns 0.0
                }
            },
        )

        assertThat(metadata.playCount).isEqualTo(100)
    }
}
