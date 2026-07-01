package de.carsten.android.muzzic.id3

import io.mockk.every
import io.mockk.mockk
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.id3.ID3v23Frame
import org.jaudiotagger.tag.id3.framebody.FrameBodyPCNT
import org.jaudiotagger.tag.id3.framebody.FrameBodyPOPM
import org.junit.Assert.assertEquals
import org.junit.Test

class Id3TagParserTest {

    @Test
    fun `parseTrackString parses track and total with various separators`() {
        assertEquals(Pair(1, 10), Id3TagParser.parseTrackString("1/10"))
        assertEquals(Pair(1, 10), Id3TagParser.parseTrackString("01/10"))
        assertEquals(Pair(2, 12), Id3TagParser.parseTrackString("2\\12"))
        assertEquals(Pair(3, 15), Id3TagParser.parseTrackString("3-15"))
        assertEquals(Pair(4, 20), Id3TagParser.parseTrackString("4:20"))
        assertEquals(Pair(5, -1), Id3TagParser.parseTrackString("5"))
        assertEquals(Pair(-1, -1), Id3TagParser.parseTrackString(""))
        assertEquals(Pair(-1, -1), Id3TagParser.parseTrackString(null))
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

        val metadata = Id3TagParser.extractMetadata(mockk {
            every { this@mockk.tag } returns tag
            every { audioHeader } returns mockk {
                every { preciseTrackLength } returns 0.0
            }
        })

        assertEquals(2023, metadata.year)
        assertEquals(128, metadata.rating)
        assertEquals(42, metadata.playCount)
        assertEquals(1, metadata.trackNumber)
        assertEquals(10, metadata.totalTracks)
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

        val metadata = Id3TagParser.extractMetadata(mockk {
            every { this@mockk.tag } returns tag
            every { audioHeader } returns mockk {
                every { preciseTrackLength } returns 0.0
            }
        })

        assertEquals(255, metadata.rating)
        assertEquals(5, metadata.trackNumber)
        assertEquals(20, metadata.totalTracks)
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

        val metadata = Id3TagParser.extractMetadata(mockk {
            every { this@mockk.tag } returns tag
            every { audioHeader } returns mockk {
                every { preciseTrackLength } returns 0.0
            }
        })

        assertEquals(100, metadata.playCount)
    }
}
