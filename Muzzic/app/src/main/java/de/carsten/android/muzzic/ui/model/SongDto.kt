package de.carsten.android.muzzic.ui.model

import android.os.Bundle
import androidx.compose.runtime.Immutable
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.StarRating
import de.carsten.android.muzzic.MAX_STARS
import de.carsten.android.muzzic.UNKNOWN
import de.carsten.android.muzzic.inferMimeType
import de.carsten.android.muzzic.model.MediaKeys
import de.carsten.android.muzzic.toPlayableUri
import de.carsten.android.muzzic.ui.utils.getStarRating
import de.carsten.android.muzzic.ui.utils.getWmpRating
import java.time.Instant

@Immutable
data class SongDto(
    val id: String = UNKNOWN,
    val title: String = UNKNOWN,
    val trackNumber: Int = 0,
    val totalTracks: Int = 0,
    val album: String = UNKNOWN,
    val albumYear: Int = -1,
    val artist: String = UNKNOWN,
    val duration: Long = 0L,
    val rating: Int = 128,
    val playCount: Int = 0,
    val lastPlayed: Instant = Instant.EPOCH,
    val genre: String = UNKNOWN,
    val albumArt: String = UNKNOWN,
    val filePath: String = UNKNOWN,
    val queuePosition: Int = 0,
    val createdAt: Instant = Instant.EPOCH,
    val updatedAt: Instant = Instant.EPOCH,
) {
    companion object {
        fun fromMediaItem(mediaItem: MediaItem): SongDto {
            val metadata = mediaItem.mediaMetadata
            return SongDto(
                id = mediaItem.mediaId,
                filePath = mediaItem.localConfiguration?.uri?.toString() ?: UNKNOWN,
                title = metadata.title.toString(),
                trackNumber = metadata.trackNumber ?: 0,
                totalTracks = metadata.totalTrackCount ?: 0,
                artist = metadata.artist.toString(),
                album = metadata.albumTitle.toString(),
                albumYear = metadata.releaseYear ?: -1,
                albumArt = metadata.artworkUri.toString(),
                genre = metadata.genre.toString(),
                duration = metadata.durationMs ?: 0L,
                rating = getWmpRating(metadata.userRating as? StarRating ?: StarRating(MAX_STARS, 0f)),
                playCount = metadata.extras?.getInt(MediaKeys.PLAY_COUNT, 0) ?: 0,
                queuePosition = metadata.extras?.getInt(MediaKeys.QUEUE_POSITION, 0) ?: 0,
            )
        }
    }

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

    fun toMediaItem(index: Int = 0): MediaItem = MediaItem
        .Builder()
        .setMediaId(this.id)
        .setUri(this.filePath.toPlayableUri())
        .setMimeType(this.filePath.inferMimeType())
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(this.title)
                .setArtist(this.artist)
                .setAlbumArtist(this.artist)
                .setAlbumTitle(this.album)
                .setTrackNumber(this.trackNumber)
                .setTotalTrackCount(this.totalTracks)
                .setReleaseYear(this.albumYear)
                .setArtworkUri(this.albumArt.toUri())
                .setGenre(this.genre)
                .setDurationMs(this.duration)
                .setUserRating(getStarRating(this.rating))
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .setExtras(getExtras(index))
                .build(),
        )
        .build()

    fun <I : Iterable<SongDto>> I.toMediaItems(): List<MediaItem> = this.mapIndexed { index, songDto -> songDto.toMediaItem(index) }

    private fun getExtras(index: Int = 0) = Bundle().apply {
        putString(MediaKeys.SONG_ID, id)
        putInt(MediaKeys.PLAY_COUNT, playCount)
        putInt(MediaKeys.QUEUE_POSITION, index)
        putLong(MediaKeys.LAST_PLAYED, lastPlayed.toEpochMilli())
        putLong(MediaKeys.CREATED_AT, createdAt.toEpochMilli())
        putLong(MediaKeys.UPDATED_AT, updatedAt.toEpochMilli())
    }
}
