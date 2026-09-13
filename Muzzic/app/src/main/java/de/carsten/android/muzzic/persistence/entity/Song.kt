package de.carsten.android.muzzic.persistence.entity

import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import de.carsten.android.muzzic.UNKNOWN
import de.carsten.android.muzzic.UNKNOWN_ALBUM
import de.carsten.android.muzzic.UNKNOWN_ARTIST
import de.carsten.android.muzzic.UNKNOWN_GENRE
import de.carsten.android.muzzic.id3.model.Id3Metadata
import de.carsten.android.muzzic.inferMimeType
import de.carsten.android.muzzic.model.MediaKeys
import de.carsten.android.muzzic.songId
import de.carsten.android.muzzic.toPlayableUri
import de.carsten.android.muzzic.ui.model.SongDto
import de.carsten.android.muzzic.ui.utils.getStarRating
import java.time.Instant

@Entity(
    tableName = "songs",
    indices = [
        Index("updatedAt"),
        Index("artist", "albumYear", "album", "trackNumber", "title"),
        Index("album", "artist"),
        Index("genre", "rating", "playCount"),
    ],
)
data class Song(
    @ColumnInfo
    val title: String = UNKNOWN,
    @ColumnInfo(defaultValue = "0")
    val trackNumber: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val totalTracks: Int = 0,
    @ColumnInfo(index = true, collate = ColumnInfo.NOCASE)
    val artist: String = UNKNOWN,
    @ColumnInfo(index = true, collate = ColumnInfo.NOCASE)
    val album: String = UNKNOWN,
    @ColumnInfo(index = true, collate = ColumnInfo.NOCASE)
    val genre: String = UNKNOWN,
    @ColumnInfo(defaultValue = "0")
    val duration: Long = 0L, // in milliseconds
    @ColumnInfo
    val filePath: String? = null,
    @ColumnInfo
    val albumArt: String? = null,
    @ColumnInfo(defaultValue = "-1")
    val albumYear: Int = -1,
    @ColumnInfo(defaultValue = "3")
    val rating: Int = 0, // 0-5 stars
    @ColumnInfo(defaultValue = "0")
    val playCount: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val lastPlayed: Instant = Instant.ofEpochMilli(0L),
) : AbstractEntity(),
    Comparable<Song> {
    init {
        this.id = songId(title, album, artist).toString()
    }

    companion object {
        /**
         * Creates a [Song] from parsed [Id3Metadata].
         *
         * Blank fields fall back to the UNKNOWN constants. The caller resolves the
         * playable [duration] (e.g. TLEN tag value with a media retriever fallback)
         * since metadata alone may not carry it.
         *
         * @param filePath playable path or content URI string of the scanned file.
         * @param metadata parsed ID3 metadata.
         * @param albumArt resolved album art URI string, if any.
         * @param duration playable duration in milliseconds.
         * @return mapped [Song].
         */
        fun fromId3(filePath: String, metadata: Id3Metadata, albumArt: String?, duration: Long = metadata.duration ?: 0L): Song = Song(
            title = metadata.title?.takeIf { it.isNotBlank() } ?: UNKNOWN,
            artist = metadata.artist?.takeIf { it.isNotBlank() } ?: UNKNOWN_ARTIST,
            album = metadata.album?.takeIf { it.isNotBlank() } ?: UNKNOWN_ALBUM,
            genre = metadata.genre?.takeIf { it.isNotBlank() } ?: UNKNOWN_GENRE,
            duration = duration,
            filePath = filePath,
            trackNumber = (metadata.track ?: -1).coerceAtLeast(0),
            totalTracks = (metadata.totalTracks ?: -1).coerceAtLeast(0),
            albumYear = metadata.albumYear ?: -1,
            rating = metadata.rating ?: 0,
            playCount = metadata.playCount ?: 0,
            albumArt = albumArt,
        )
    }

    override fun compareTo(other: Song): Int {
        val c1 = this.artist.compareTo(other.artist)
        return if (c1 != 0) {
            c1
        } else {
            val c2 = this.album.compareTo(other.album)
            if (c2 != 0) {
                c2
            } else {
                val c3 = if (this.albumYear > other.albumYear) {
                    1
                } else if (this.albumYear < other.albumYear) {
                    -1
                } else {
                    0
                }
                if (c3 != 0) {
                    c3
                } else {
                    val c4 = this.trackNumber.compareTo(other.trackNumber)
                    if (c4 != 0) {
                        c4
                    } else {
                        this.title.compareTo(other.title)
                    }
                }
            }
        }
    }

    /**
     * Maps the current [Song] to a [SongDto]
     */
    fun toDto(): SongDto = SongDto(
        id = id,
        title = title,
        trackNumber = trackNumber,
        totalTracks = totalTracks,
        artist = artist,
        album = album,
        genre = genre,
        duration = duration,
        rating = rating,
        playCount = playCount,
        lastPlayed = lastPlayed,
        filePath = filePath ?: UNKNOWN,
        albumArt = albumArt ?: UNKNOWN,
        createdAt = createdAt ?: Instant.EPOCH,
        updatedAt = updatedAt ?: Instant.EPOCH,
    )

    fun toMediaItem(index: Int? = null): MediaItem = MediaItem
        .Builder()
        .setMediaId(this.id)
        .setUri(this.filePath.toPlayableUri())
        .setMimeType(this.filePath.inferMimeType())
        .setMediaMetadata(
            MediaMetadata
                .Builder()
                .setTitle(this.title)
                .setTrackNumber(this.trackNumber)
                .setTotalTrackCount(this.totalTracks)
                .setArtist(this.artist)
                .setAlbumTitle(this.album)
                .setReleaseYear(this.albumYear)
                .setAlbumArtist(this.artist)
                .setArtworkUri(this.albumArt?.toUri())
                .setGenre(this.genre)
                .setDurationMs(this.duration)
                .setUserRating(getStarRating(this.rating))
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .setExtras(getExtras(index))
                .build(),
        ).build()

    private fun getExtras(index: Int?) = Bundle().apply {
        putString("songId", id)
        putInt(MediaKeys.PLAY_COUNT, playCount)
        putInt(MediaKeys.QUEUE_POSITION, index ?: -1)
        putLong(MediaKeys.LAST_PLAYED, lastPlayed.toEpochMilli())
        putLong(MediaKeys.CREATED_AT, createdAt?.toEpochMilli() ?: Instant.EPOCH.toEpochMilli())
        putLong(MediaKeys.UPDATED_AT, updatedAt?.toEpochMilli() ?: Instant.EPOCH.toEpochMilli())
    }
}
