package de.carsten.android.muzzic.persistence.entity

import android.os.Bundle
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.StarRating
import androidx.room.ColumnInfo
import androidx.room.Entity
import de.carsten.android.muzzic.utils.maxStars
import de.carsten.android.muzzic.utils.mediaItemInstant
import java.time.Instant

@Entity(tableName = "songs")
data class Song(
    @ColumnInfo
    val title: String? = null,
    @ColumnInfo(defaultValue = "0")
    val trackNumber: Int? = 0,
    @ColumnInfo(defaultValue = "0")
    val totalTracks: Int? = 0,
    @ColumnInfo(index = true)
    val artist: String? = null,
    @ColumnInfo(index = true)
    val album: String? = null,
    @ColumnInfo(index = true)
    val genre: String? = null,
    @ColumnInfo(defaultValue = "0")
    val duration: Long? = 0L, // in milliseconds
    @ColumnInfo
    val filePath: String? = null,
    @ColumnInfo
    val albumArt: String? = null,
    @ColumnInfo(defaultValue = "-1")
    val albumYear: Int? = -1,
    @ColumnInfo(defaultValue = "3")
    val rating: Int? = 0, // 0-5 stars
    @ColumnInfo(defaultValue = "0")
    val playCount: Int? = 0,
    @ColumnInfo(defaultValue = "0")
    val lastPlayed: Instant? = Instant.ofEpochMilli(0L),
) : AbstractEntity() {
    companion object {
        fun fromMediaItem(mediaItem: MediaItem): Song {
            val metadata = mediaItem.mediaMetadata
            return Song(
                trackNumber = metadata.trackNumber,
                totalTracks = metadata.totalTrackCount,
                title = metadata.title?.toString(),
                artist = metadata.artist?.toString(),
                album = metadata.albumTitle?.toString(),
                filePath = metadata.artworkUri?.toString(),
                albumArt = metadata.artworkUri?.toString(),
                albumYear = metadata.releaseYear,
                rating = getWmpRating(metadata.userRating as StarRating),
                genre = metadata.genre?.toString(),
                duration = metadata.durationMs,
                lastPlayed = mediaItem.mediaItemInstant("lastPlayed"),
            ).apply {
                id = mediaItem.mediaId
                createdAt = mediaItem.mediaItemInstant("createdAt")
                updatedAt = mediaItem.mediaItemInstant("updatedAt")
            }
        }

        /**
         * gets star rating
         *
         * @param rating [Int] wmp9 rating (0..255) integer value
         * @return [StarRating] rating in the range 0f..5f
         */
        fun getStarRating(rating: Int? = 128): StarRating =
            StarRating(
                maxStars,
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
        fun getWmpRating(starRating: StarRating): Int =
            when (starRating.starRating) {
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

    fun toMediaItem(): MediaItem =
        MediaItem
            .Builder()
            .setMediaId(this.id)
            .setUri(this.filePath)
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
                    .setArtworkUri(this.filePath?.toUri())
                    .setGenre(this.genre)
                    .setDurationMs(this.duration)
                    .setUserRating(getStarRating(this.rating))
                    .setExtras(getExtras())
                    .build(),
            ).build()

    private fun getExtras(): Bundle =
        bundleOf(
            Pair("playCount", this.playCount ?: 0),
            Pair("lastPlayed", this.lastPlayed?.toEpochMilli() ?: Instant.now().toEpochMilli()),
            Pair("createdAt", this.createdAt?.toEpochMilli() ?: Instant.now().toEpochMilli()),
            Pair("updatedAt", this.updatedAt?.toEpochMilli() ?: Instant.now().toEpochMilli()),
        )
}
