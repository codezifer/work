package de.carsten.android.muzzic.persistence.entity

import androidx.media3.common.MediaItem
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import de.carsten.android.muzzic.model.MediaKeys

/**
 * Persisted playback queue entry.
 *
 * Holds only the reference to the library song ([songId]) plus the queue state
 * ([enqueued], [queuePosition]); the surrogate [AbstractEntity.id] remains the
 * primary key and [AbstractTimestampEntity.createdAt]/[AbstractTimestampEntity.updatedAt]
 * keep the entry timestamps. All song metadata is resolved at read time by
 * joining against `songs` (see `PlayingQueueSongAggregation`), so the `songs`
 * table stays the single source of truth.
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
data class PlayingQueue(val songId: String, var enqueued: Boolean = false, var queuePosition: Int = 0) : AbstractEntity() {
    companion object {
        const val TABLE_NAME = "playing_queue"

        /**
         * Creates a [PlayingQueue] entry from a [MediaItem].
         *
         * The song id is read from the media item's extras and falls back to the
         * media id. The enqueued flag and queue position are supplied by the
         * caller; a non-blank media id becomes the entity id.
         *
         * @param mediaItem the item being persisted.
         * @param enqueued whether the entry belongs to the active playback queue.
         * @param queuePosition the position inside the queue.
         * @return the mapped queue entry.
         */
        fun fromMediaItem(mediaItem: MediaItem, enqueued: Boolean, queuePosition: Int): PlayingQueue = PlayingQueue(
            songId = mediaItem.mediaMetadata.extras?.getString(MediaKeys.SONG_ID).orEmpty().ifBlank { mediaItem.mediaId },
            enqueued = enqueued,
            queuePosition = queuePosition,
        ).apply {
            if (mediaItem.mediaId.isNotBlank()) {
                id = mediaItem.mediaId
            }
        }
    }
}
