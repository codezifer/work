package de.carsten.android.muzzic.persistence.entity

import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.room.Entity

@Entity(tableName = "playing_queue")
data class PlayingQueue(
    val title: String? = null,
    val trackNumber: Int? = null,
    val totalTracks: Int? = null,
    val artist: String? = null,
    val album: String? = null,
    val albumArt: String? = null,
    val genre: String? = null,
    val duration: Long? = null, // in milliseconds
    var queuePosition: Int = 0,
) : AbstractEntity() {
    companion object {
        fun fromMediaItem(mediaItem: MediaItem): PlayingQueue =
            PlayingQueue(
                title = mediaItem.mediaMetadata.title?.toString(),
                trackNumber = mediaItem.mediaMetadata.trackNumber,
                totalTracks = mediaItem.mediaMetadata.totalTrackCount,
                artist = mediaItem.mediaMetadata.artist?.toString(),
                album = mediaItem.mediaMetadata.albumTitle?.toString(),
                albumArt = mediaItem.mediaMetadata.artworkUri?.toString(),
                genre = mediaItem.mediaMetadata.genre?.toString(),
            )
    }

    fun toMediaItem(): MediaItem =
        MediaItem
            .Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata
                    .Builder()
                    .setTitle(title)
                    .setTrackNumber(trackNumber)
                    .setTotalTrackCount(totalTracks)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setArtworkUri(albumArt?.toUri())
                    .setDurationMs(duration)
                    .setExtras(getExtras())
                    .build(),
            ).build()

    private fun getExtras(): Bundle =
        Bundle().apply {
            putInt("queuePosition", queuePosition)
            putLong("createdAt", createdAt?.toEpochMilli() ?: 0L)
            putLong("updatedAt", updatedAt?.toEpochMilli() ?: 0L)
        }
}
