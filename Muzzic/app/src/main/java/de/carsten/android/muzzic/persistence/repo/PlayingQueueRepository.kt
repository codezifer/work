package de.carsten.android.muzzic.persistence.repo

import androidx.media3.common.MediaItem
import de.carsten.android.muzzic.persistence.dao.PlayingQueueDao
import de.carsten.android.muzzic.persistence.entity.PlayingQueue

class PlayingQueueRepository(
    private val playingQueueDao: PlayingQueueDao,
) {
    suspend fun getPlayingQueue(): List<MediaItem> = playingQueueDao.findAll().map { song -> song.toMediaItem() }

    suspend fun clear() {
        playingQueueDao.clearQueue()
    }

    suspend fun addSongs(mediaItems: List<MediaItem>) {
        playingQueueDao.addSongs(
            mediaItems.map { item ->
                PlayingQueue.fromMediaItem(item)
            },
        )
    }

    suspend fun removeSongs(mediaItems: List<MediaItem>) {
        playingQueueDao.removeSongs(mediaItems.map { item -> item.mediaId })
    }
}
