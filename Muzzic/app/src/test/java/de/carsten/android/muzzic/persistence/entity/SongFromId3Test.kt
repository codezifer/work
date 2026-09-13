package de.carsten.android.muzzic.persistence.entity

import de.carsten.android.muzzic.UNKNOWN
import de.carsten.android.muzzic.UNKNOWN_ALBUM
import de.carsten.android.muzzic.UNKNOWN_ARTIST
import de.carsten.android.muzzic.UNKNOWN_GENRE
import de.carsten.android.muzzic.id3.model.AlbumArtMetadata
import de.carsten.android.muzzic.id3.model.Id3Metadata
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * Unit tests for [Song.fromId3] with [Id3Metadata].
 */
class SongFromId3Test {

    @Test
    fun `fromId3 maps metadata fields`() {
        val metadata = Id3Metadata(
            title = "Song",
            track = 4,
            totalTracks = 9,
            artist = "Artist",
            album = "Album",
            albumYear = 2023,
            albumArt = AlbumArtMetadata(offset = 100, length = 50, mimeType = "image/jpeg", hashCode = 7),
            genre = "Rock",
            rating = 200,
            playCount = 42,
            duration = 187000L,
        )

        val song = Song.fromId3("content://media/song.mp3", metadata, "albumart://cover", 187000L)

        assertThat(song.title).isEqualTo("Song")
        assertThat(song.artist).isEqualTo("Artist")
        assertThat(song.album).isEqualTo("Album")
        assertThat(song.genre).isEqualTo("Rock")
        assertThat(song.duration).isEqualTo(187000L)
        assertThat(song.filePath).isEqualTo("content://media/song.mp3")
        assertThat(song.trackNumber).isEqualTo(4)
        assertThat(song.totalTracks).isEqualTo(9)
        assertThat(song.albumYear).isEqualTo(2023)
        assertThat(song.rating).isEqualTo(200)
        assertThat(song.playCount).isEqualTo(42)
        assertThat(song.albumArt).isEqualTo("albumart://cover")
    }

    @Test
    fun `fromId3 falls back to unknown constants and defaults`() {
        val song = Song.fromId3("content://media/untagged.mp3", Id3Metadata(), null)

        assertThat(song.title).isEqualTo(UNKNOWN)
        assertThat(song.artist).isEqualTo(UNKNOWN_ARTIST)
        assertThat(song.album).isEqualTo(UNKNOWN_ALBUM)
        assertThat(song.genre).isEqualTo(UNKNOWN_GENRE)
        assertThat(song.duration).isEqualTo(0L)
        assertThat(song.filePath).isEqualTo("content://media/untagged.mp3")
        assertThat(song.trackNumber).isEqualTo(0)
        assertThat(song.totalTracks).isEqualTo(0)
        assertThat(song.albumYear).isEqualTo(-1)
        assertThat(song.rating).isEqualTo(0)
        assertThat(song.playCount).isEqualTo(0)
        assertThat(song.albumArt).isNull()
    }

    @Test
    fun `fromId3 uses explicit duration over tag duration`() {
        val metadata = Id3Metadata(title = "Song", duration = 1000L)

        val song = Song.fromId3("content://media/song.mp3", metadata, null, duration = 200000L)

        assertThat(song.duration).isEqualTo(200000L)
    }
}
