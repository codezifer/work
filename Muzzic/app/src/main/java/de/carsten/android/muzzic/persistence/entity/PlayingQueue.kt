package de.carsten.android.muzzic.persistence.entity

import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import de.carsten.android.muzzic.EMPTY
import de.carsten.android.muzzic.UNKNOWN
import de.carsten.android.muzzic.inferMimeType
import de.carsten.android.muzzic.toPlayableUri
import de.carsten.android.muzzic.ui.model.PlayingQueueDto

@Entity(
    tableName = "playing_queue",
    foreignKeys = [
        ForeignKey(
            entity = Song::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("songId"),
    ],
)
data class PlayingQueue(
    val title: String? = null,
    val trackNumber: Int? = null,
    val totalTracks: Int? = null,
    val artist: String? = null,
    val album: String? = null,
    val albumYear: Int? = null,
    val albumArt: String? = null,
    val genre: String? = null,
    val duration: Long? = null, // in milliseconds
    val filePath: String? = null,
    var enqueued: Boolean? = null,
    val songId: String? = null,
    var queuePosition: Int = 0,
) : AbstractEntity() {
    companion object {
        fun fromMediaItem(mediaItem: MediaItem) = PlayingQueue(
            title = mediaItem.mediaMetadata.title?.toString(),
            trackNumber = mediaItem.mediaMetadata.trackNumber,
            totalTracks = mediaItem.mediaMetadata.totalTrackCount,
            artist = mediaItem.mediaMetadata.artist?.toString(),
            album = mediaItem.mediaMetadata.albumTitle?.toString(),
            albumYear = mediaItem.mediaMetadata.releaseYear,
            albumArt = mediaItem.mediaMetadata.artworkUri?.toString(),
            genre = mediaItem.mediaMetadata.genre?.toString(),
            duration = mediaItem.mediaMetadata.durationMs,
            filePath = mediaItem.localConfiguration?.uri?.toString(),
            enqueued = mediaItem.mediaMetadata.extras?.getBoolean("enqueued"),
            songId =
            mediaItem.mediaMetadata.extras?.getString("songId").let {
                if (it.isNullOrBlank()) null else it
            },
            queuePosition = mediaItem.mediaMetadata.extras?.getInt("queuePosition") ?: 0,
        ).apply {
            if (mediaItem.mediaId.isNotBlank()) {
                id = mediaItem.mediaId
            }
        }

        fun fromDto(dto: PlayingQueueDto) = PlayingQueue(
            title = dto.title,
            trackNumber = dto.trackNumber,
            totalTracks = dto.totalTracks,
            artist = dto.artist,
            album = dto.album,
            albumYear = dto.albumYear,
            albumArt = dto.albumArt,
            genre = dto.genre,
            duration = dto.duration,
            filePath = dto.filePath,
            enqueued = dto.enqueued,
        )
    }

    fun toMediaItem(): MediaItem = MediaItem
        .Builder()
        .setMediaId(id)
        .setUri(filePath.toPlayableUri())
        .setMimeType(filePath.inferMimeType())
        .setMediaMetadata(
            MediaMetadata
                .Builder()
                .setTitle(title)
                .setTrackNumber(trackNumber)
                .setTotalTrackCount(totalTracks)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setReleaseYear(albumYear)
                .setArtworkUri(albumArt?.toUri())
                .setGenre(genre)
                .setDurationMs(duration)
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .setExtras(getExtras())
                .build(),
        ).build()

    fun toDto() = PlayingQueueDto(
        title = title ?: UNKNOWN,
        trackNumber = trackNumber ?: -1,
        totalTracks = totalTracks ?: -1,
        artist = artist ?: UNKNOWN,
        album = album ?: UNKNOWN,
        albumYear = albumYear ?: -1,
        albumArt = albumArt ?: UNKNOWN,
        genre = genre ?: UNKNOWN,
        duration = duration ?: -1L,
        filePath = filePath ?: EMPTY,
        enqueued = false,
        queuePosition = queuePosition,
        songId = songId ?: EMPTY,
        mediaId = id,
    )

    private fun getExtras(): Bundle = Bundle().apply {
        putInt("queuePosition", queuePosition)
        putBoolean("enqueued", enqueued ?: false)
        putString("songId", songId ?: "")
        putLong("createdAt", createdAt?.toEpochMilli() ?: 0L)
        putLong("updatedAt", updatedAt?.toEpochMilli() ?: 0L)
    }
}
