package de.carsten.android.muzzic.persistence.repo

import androidx.media3.common.MediaItem
import de.carsten.android.muzzic.persistence.dao.PlayingQueueDao
import de.carsten.android.muzzic.persistence.entity.PlayingQueue
import de.carsten.android.muzzic.ui.model.PlayingQueueDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlayingQueueRepository(private val playingQueueDao: PlayingQueueDao) {
    suspend fun getPlayingQueue(): List<MediaItem> = playingQueueDao.findEnqueued().map { song -> song.toMediaItem() }

    suspend fun getCompleteQueue(): List<MediaItem> = playingQueueDao.findAll().map { it.toMediaItem() }

    fun observePlayingQueue(): Flow<List<PlayingQueueDto>> = playingQueueDao.observeAll().map { queue ->
        queue.map { it.toDto() }
    }

    fun observeEnqueued(): Flow<List<MediaItem>> = playingQueueDao.observeEnqueued().map { queue ->
        queue.map { it.toMediaItem() }
    }

    suspend fun clear() {
        playingQueueDao.clearQueue()
    }

    suspend fun addSongs(mediaItems: List<MediaItem>, enqueued: Boolean = true): List<MediaItem> {
        val entities =
            mediaItems.map { item ->
                PlayingQueue.fromMediaItem(item).apply {
                    this.enqueued = enqueued
                }
            }
        playingQueueDao.addSongs(entities)
        return entities.map { it.toMediaItem() }
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
