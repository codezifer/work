package de.carsten.android.muzzic.service

import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import de.carsten.android.muzzic.logging.logger
import de.carsten.android.muzzic.persistence.repo.PlayingQueueRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Manages the playback queue and synchronizes it with the database.
 */
class QueueManager(private val playingQueueRepository: PlayingQueueRepository) {
    private val logger = logger()

    /**
     * Loads the persisted queue from the database and sets it on the player.
     */
    fun loadPersistedQueue(scope: CoroutineScope, player: ExoPlayer) {
        scope.launch {
            val queue = playingQueueRepository.getPlayingQueue()
            if (queue.isNotEmpty()) {
                player.setMediaItems(queue)
                player.prepare()
                logger.info("Restored ${queue.size} items to the playing queue")
            }
        }
    }

    /**
     * Observes the database for queue changes and synchronizes the player.
     */
    fun observeQueueChanges(scope: CoroutineScope, player: ExoPlayer) {
        scope.launch {
            playingQueueRepository.observeEnqueued().collect { queue ->
                syncPlayerWithDb(player, queue)
            }
        }
    }

    private fun syncPlayerWithDb(player: ExoPlayer, queue: List<MediaItem>) {
        val currentPlayerItems = mutableListOf<MediaItem>()
        for (i in 0 until player.mediaItemCount) {
            currentPlayerItems.add(player.getMediaItemAt(i))
        }

        if (currentPlayerItems.size == queue.size &&
            currentPlayerItems.zip(queue).all { (p, d) -> p.mediaId == d.mediaId }
        ) {
            return
        }

        logger.info("Syncing player with database queue (${queue.size} items)")

        val currentMediaItem = player.currentMediaItem
        val currentPosition = player.currentPosition
        val wasPlaying = player.isPlaying

        val newIndex =
            if (currentMediaItem != null) {
                queue.indexOfFirst { it.mediaId == currentMediaItem.mediaId }
            } else {
                -1
            }

        if (newIndex != -1) {
            player.setMediaItems(queue, newIndex, currentPosition)
        } else {
            player.setMediaItems(queue)
        }

        player.prepare()
        if (wasPlaying) {
            player.play()
        }
    }

    fun setPlaylist(player: ExoPlayer, newPlaylist: List<MediaItem>, startIndex: Int = 0) {
        if (newPlaylist.isNotEmpty() && startIndex in newPlaylist.indices) {
            player.setMediaItems(newPlaylist, startIndex, 0L)
            player.prepare()
        } else {
            player.clearMediaItems()
            player.stop()
        }
    }

    fun addMediaItem(player: ExoPlayer, mediaItem: MediaItem) {
        player.addMediaItem(mediaItem)
    }

    fun playAt(player: ExoPlayer, index: Int) {
        if (index in 0 until player.mediaItemCount) {
            player.seekToDefaultPosition(index)
            player.prepare()
            player.play()
        }
    }
}
