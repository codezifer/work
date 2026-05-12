package de.carsten.android.muzzic.persistence.repo

import androidx.media3.common.MediaItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import de.carsten.android.muzzic.persistence.dao.PlayingQueueDao
import de.carsten.android.muzzic.persistence.entity.PlayingQueue

class PlayingQueueRepository(
    private val playingQueueDao: PlayingQueueDao,
) {
    suspend fun getPlayingQueue(): List<MediaItem> = playingQueueDao.findAll().map { song -> song.toMediaItem() }

    fun observePlayingQueue(): Flow<List<MediaItem>> =
        playingQueueDao.observeAll().map { queue ->
            queue.map { it.toMediaItem() }
        }

    suspend fun clear() {
        playingQueueDao.clearQueue()
    }

    suspend fun addSongs(mediaItems: List<MediaItem>, enqueued: Boolean = true): List<PlayingQueue> {
        val entities = mediaItems.map { item ->
            PlayingQueue.fromMediaItem(item).apply {
                this.enqueued = enqueued
            }
        }
        playingQueueDao.addSongs(entities)
        return entities
    }

    suspend fun removeSongs(mediaItems: List<MediaItem>) {
        playingQueueDao.removeSongs(mediaItems.map { item -> item.mediaId })
    }

    suspend fun persistQueue(mediaItems: List<MediaItem>) {
        playingQueueDao.addSongs(
            mediaItems.mapIndexed { index, item ->
                PlayingQueue.fromMediaItem(item).apply {
                    this.queuePosition = index
                    this.enqueued = true
                }
            },
        )
    }
}
