package de.carsten.android.muzzic.persistence.entity

import androidx.media3.common.MediaItem
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import de.carsten.android.muzzic.model.MediaKeys

/**
 * Persisted playback queue entry.
 *
 * Mirrors the columns needed to restore a playable [MediaItem] after a process
 * restart. Song-level values such as the rating are not stored here; they are
 * resolved at read time by joining against `songs`
 * (see `PlayingQueueSongAggregation`).
 */
@Entity(
    tableName = PlayingQueue.TABLE_NAME,
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
        const val TABLE_NAME = "playing_queue"

        /**
         * Creates a [PlayingQueue] entry from a [MediaItem].
         *
         * Queue position, enqueued flag and song id are read from the media item's
         * extras; a non-blank media id becomes the entity id.
         *
         * @param mediaItem the item being persisted.
         * @return the mapped queue entry.
         */
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
            enqueued = mediaItem.mediaMetadata.extras?.getBoolean(MediaKeys.ENQUEUED),
            songId =
                mediaItem.mediaMetadata.extras?.getString(MediaKeys.SONG_ID).let {
                    if (it.isNullOrBlank()) null else it
                },
            queuePosition = mediaItem.mediaMetadata.extras?.getInt(MediaKeys.QUEUE_POSITION) ?: 0,
        ).apply {
            if (mediaItem.mediaId.isNotBlank()) {
                id = mediaItem.mediaId
            }
        }
    }

}
