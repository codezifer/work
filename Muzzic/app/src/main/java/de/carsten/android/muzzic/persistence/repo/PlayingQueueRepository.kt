package de.carsten.android.muzzic.persistence.repo

import androidx.media3.common.MediaItem
import de.carsten.android.muzzic.persistence.dao.PlayingQueueDao
import de.carsten.android.muzzic.persistence.entity.PlayingQueue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class PlayingQueueRepository(private val playingQueueDao: PlayingQueueDao) {

    fun getPlayingQueue(): Flow<List<MediaItem>> = flow {
        playingQueueDao.findAll().map { song -> song.toMediaItem() }
    }

    fun clear(): Flow<Unit> = flow {
        playingQueueDao.clearQueue()
    }

    fun addSongs(mediaItems: List<MediaItem>): Flow<Unit> = flow {
        playingQueueDao.addSongs(mediaItems.map { item ->
            PlayingQueue.fromMediaItem(item)
        })
    }

    fun removeSongs(mediaItems: List<MediaItem>): Flow<Unit> = flow {
        playingQueueDao.removeSongs(mediaItems.map { item -> item.mediaId })
    }
}
