package de.carsten.android.muzzic.persistence.entity

import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.StarRating
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import de.carsten.android.muzzic.MAX_STARS
import de.carsten.android.muzzic.UNKNOWN
import de.carsten.android.muzzic.UNKNOWN_ALBUM
import de.carsten.android.muzzic.UNKNOWN_ARTIST
import de.carsten.android.muzzic.id3.ExtendedMetadata
import de.carsten.android.muzzic.inferMimeType
import de.carsten.android.muzzic.mediaItemInstant
import de.carsten.android.muzzic.model.MediaKeys
import de.carsten.android.muzzic.songId
import de.carsten.android.muzzic.toPlayableUri
import java.io.File
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
        fun fromMediaItem(mediaItem: MediaItem): Song {
            val metadata = mediaItem.mediaMetadata
            return Song(
                trackNumber = metadata.trackNumber ?: 0,
                totalTracks = metadata.totalTrackCount ?: 0,
                title = metadata.title?.toString()?.takeIf { it.isNotBlank() } ?: UNKNOWN,
                artist = metadata.artist?.toString()?.takeIf { it.isNotBlank() } ?: UNKNOWN_ARTIST,
                album = metadata.albumTitle?.toString()?.takeIf { it.isNotBlank() } ?: UNKNOWN_ALBUM,
                genre = metadata.genre?.toString()?.takeIf { it.isNotBlank() } ?: UNKNOWN,
                filePath = mediaItem.localConfiguration?.uri?.toString(),
                albumArt = metadata.artworkUri?.toString(),
                albumYear = metadata.releaseYear ?: -1,
                rating = getWmpRating(metadata.userRating as? StarRating ?: StarRating(MAX_STARS, 0f)),
                duration = metadata.durationMs ?: 0L,
                playCount = metadata.extras?.getInt(MediaKeys.PLAY_COUNT) ?: 0,
                lastPlayed = mediaItem.mediaItemInstant(MediaKeys.LAST_PLAYED),
            ).apply {
                id = mediaItem.mediaId
                createdAt = mediaItem.mediaItemInstant(MediaKeys.CREATED_AT)
                updatedAt = mediaItem.mediaItemInstant(MediaKeys.UPDATED_AT)
            }
        }

        fun fromId3(file: File, metadata: ExtendedMetadata, albumArt: String?): Song = Song(
            title = metadata.title?.takeIf { it.isNotBlank() } ?: UNKNOWN,
            artist = metadata.artist?.takeIf { it.isNotBlank() } ?: UNKNOWN_ARTIST,
            album = metadata.album?.takeIf { it.isNotBlank() } ?: UNKNOWN_ALBUM,
            genre = metadata.genre?.takeIf { it.isNotBlank() } ?: UNKNOWN,
            duration = metadata.duration,
            filePath = file.absolutePath,
            trackNumber = metadata.trackNumber.coerceAtLeast(0),
            totalTracks = metadata.totalTracks.coerceAtLeast(0),
            albumYear = metadata.year,
            rating = metadata.rating,
            playCount = metadata.playCount,
            albumArt = albumArt,
        )

        /**
         * gets star rating
         *
         * @param rating [Int] wmp9 rating (0..255) integer value
         * @return [StarRating] rating in the range 0f..5f
         */
        fun getStarRating(rating: Int? = 128): StarRating = StarRating(
            MAX_STARS,
            when (rating) {
                0 -> 0f
                in 1..25 -> 0.5f
                in 25..51 -> 1.0f
                in 52..75 -> 1.5f
                in 76..102 -> 2.0f
                in 103..128 -> 2.5f
                in 129..153 -> 3.0f
                in 154..178 -> 3.5f
                in 179..204 -> 4.0f
                in 205..225 -> 4.5f
                in 226..255 -> 5.0f
                else -> 2.5f // default rating
            },
        )

        /**
         * gets wmp rating
         *
         * @param starRating [StarRating] current star / float based rating (0..5f)
         * @return [Int] ]rating in the range of 0..255
         */
        fun getWmpRating(starRating: StarRating): Int = when (starRating.starRating) {
            0f -> 0
            in 0f..0.5f -> 25
            in 0.6f..1.0f -> 51
            in 1.1f..1.5f -> 75
            in 1.6f..2.0f -> 102
            in 2.1f..2.5f -> 128
            in 2.6f..3.0f -> 153
            in 3.1f..3.5f -> 178
            in 3.6f..4.0f -> 204
            in 4.1f..4.5f -> 225
            in 4.6f..5.0f -> 255
            else -> 128 // default rating
        }
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

    /**
     * Format track number to string with optional total tracks (default = false)
     *
     * @param withTotal flag for format with total track (default = false) as [Boolean]
     * @return [String]
     */
    fun trackNumberFormatted(withTotal: Boolean = false): String = if (withTotal) {
        "%02d/%02d".format(trackNumber, totalTracks)
    } else {
        "%02d".format(trackNumber)
    }

    private fun getExtras(index: Int?) = Bundle().apply {
        putString("songId", id)
        putInt(MediaKeys.PLAY_COUNT, playCount)
        putInt(MediaKeys.QUEUE_POSITION, index ?: -1)
        putLong(MediaKeys.LAST_PLAYED, lastPlayed.toEpochMilli())
        putLong(MediaKeys.CREATED_AT, createdAt?.toEpochMilli() ?: Instant.now().toEpochMilli())
        putLong(MediaKeys.UPDATED_AT, updatedAt?.toEpochMilli() ?: Instant.now().toEpochMilli())
    }
}
