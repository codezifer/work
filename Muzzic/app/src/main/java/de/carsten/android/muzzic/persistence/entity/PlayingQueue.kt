package de.carsten.android.muzzic.persistence.entity

import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "playing_queue",
    foreignKeys = [
        ForeignKey(
            entity = Song::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index("songId"),
    ]
)
data class PlayingQueue(
    val title: String? = null,
    val trackNumber: Int? = null,
    val totalTracks: Int? = null,
    val artist: String? = null,
    val album: String? = null,
    val albumArt: String? = null,
    val genre: String? = null,
    val duration: Long? = null, // in milliseconds
    val filePath: String? = null,
    var enqueued: Boolean? = null,
    val songId: String? = null,
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
                duration = mediaItem.mediaMetadata.durationMs,
                filePath = mediaItem.localConfiguration?.uri?.toString(),
                enqueued = mediaItem.mediaMetadata.extras?.getBoolean("enqueued"),
                songId = mediaItem.mediaMetadata.extras?.getString("songId").let {
                    if (it.isNullOrBlank()) null else it
                },
                queuePosition = mediaItem.mediaMetadata.extras?.getInt("queuePosition") ?: 0,
            ).apply {
                if (mediaItem.mediaId.isNotBlank()) {
                    id = mediaItem.mediaId
                }
            }
    }

    fun toMediaItem(): MediaItem =
        MediaItem
            .Builder()
            .setMediaId(id)
            .setUri(filePath)
            .setMediaMetadata(
                MediaMetadata
                    .Builder()
                    .setTitle(title)
                    .setTrackNumber(trackNumber)
                    .setTotalTrackCount(totalTracks)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setArtworkUri(albumArt?.toUri())
                    .setGenre(genre)
                    .setDurationMs(duration)
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .setExtras(getExtras())
                    .build(),
            ).build()

    private fun getExtras(): Bundle =
        Bundle().apply {
            putInt("queuePosition", queuePosition)
            putBoolean("enqueued", enqueued ?: false)
            putString("songId", songId ?: "")
            putLong("createdAt", createdAt?.toEpochMilli() ?: 0L)
            putLong("updatedAt", updatedAt?.toEpochMilli() ?: 0L)
        }
}
