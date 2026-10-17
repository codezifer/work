package de.carsten.android.muzzic.persistence.repo

import androidx.media3.common.MediaItem
import de.carsten.android.muzzic.model.MediaKeys
import de.carsten.android.muzzic.persistence.dao.PlayingQueueDao
import de.carsten.android.muzzic.persistence.entity.PlayingQueue
import de.carsten.android.muzzic.ui.model.SongDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository over [PlayingQueueDao] that maps persisted queue rows to playable
 * [MediaItem]s.
 *
 * The queue stores only song references; every playable value is joined from the
 * `songs` table, so reads always reflect the current library state (rating, play
 * count, metadata).
 */
class PlayingQueueRepository(private val playingQueueDao: PlayingQueueDao) {
    /**
     * Loads the enqueued queue including the song's metadata.
     *
     * @return playable [MediaItem]s ordered by queue position.
     */
    suspend fun getPlayingQueue(): List<MediaItem> = playingQueueDao.findEnqueued().map { queueItem -> queueItem.toMediaItem() }

    /**
     * Loads the complete queue, enqueued entries first.
     *
     * @return playable [MediaItem]s for the whole queue.
     */
    suspend fun getCompleteQueue(): List<MediaItem> = playingQueueDao.findAll().map { it.toMediaItem() }

    /**
     * Observes the complete queue as UI models.
     *
     * @return a flow of the queue mapped to [SongDto].
     */
    fun observePlayingQueue(): Flow<List<SongDto>> = playingQueueDao.observeAll().map { queue ->
        queue.map { it.toSongDto() }
    }

    /**
     * Observes the enqueued queue as playable [MediaItem]s.
     *
     * @return a flow of the enqueued queue.
     */
    fun observeEnqueued(): Flow<List<MediaItem>> = playingQueueDao.observeEnqueued().map { queue ->
        queue.map { it.toMediaItem() }
    }

    /**
     * Removes every entry from the persisted queue.
     */
    suspend fun clear() {
        playingQueueDao.clearQueue()
    }

    /**
     * Persists [mediaItems] as queue entries.
     *
     * @param mediaItems items to persist; the list order becomes the queue position.
     * @param enqueued whether the persisted entries are marked as enqueued.
     * @return the persisted entries mapped back to [MediaItem]s.
     */
    suspend fun addSongs(mediaItems: List<MediaItem>, enqueued: Boolean = true): List<MediaItem> {
        val entities =
            mediaItems.mapIndexed { index, item ->
                PlayingQueue.fromMediaItem(item, enqueued = enqueued, queuePosition = index)
            }.filter { it.songId.isNotBlank() }
        playingQueueDao.addSongs(entities)
        return mediaItems.mapIndexed { index, item ->
            item.mediaMetadata.extras?.putInt(MediaKeys.QUEUE_POSITION, index)
            item.mediaMetadata.extras?.putBoolean(MediaKeys.ENQUEUED, enqueued)
            item
        }
    }

    /**
     * Removes the queue entries whose song id appears in [songIds].
     *
     * @param songIds song ids to delete from the queue.
     */
    suspend fun removeSongs(songIds: List<String>) {
        playingQueueDao.removeSongs(songIds)
    }

    /**
     * Replaces the persisted queue with [mediaItems], marking every entry as enqueued.
     *
     * @param mediaItems the queue to persist; the list order becomes the queue position.
     */
    suspend fun persistQueue(mediaItems: List<MediaItem>) {
        playingQueueDao.addSongs(
            mediaItems.mapIndexed { index, item ->
                PlayingQueue.fromMediaItem(item, enqueued = true, queuePosition = index)
            }.filter { it.songId.isNotBlank() },
        )
    }
}
