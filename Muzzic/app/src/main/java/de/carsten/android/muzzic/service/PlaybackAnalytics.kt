package de.carsten.android.muzzic.service

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import de.carsten.android.muzzic.persistence.repo.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Handles recording play counts and other playback metrics.
 */
class PlaybackAnalytics(
    private val musicRepository: MusicRepository
) {
    private var isCurrentSongCounted = false

    /**
     * Resets the play count flag for a new song.
     */
    fun onNewSong() {
        isCurrentSongCounted = false
    }

    /**
     * Starts a monitoring loop to check if the playback threshold has been reached.
     */
    fun startMonitoring(scope: CoroutineScope, player: Player) {
        scope.launch {
            while (isActive) {
                if (player.isPlaying) {
                    val currentPos = player.currentPosition
                    val duration = player.duration
                    val currentSong = player.currentMediaItem

                    if (!isCurrentSongCounted && duration > 0 && currentPos >= duration / 2) {
                        isCurrentSongCounted = true
                        currentSong?.mediaId?.let { songId ->
                            scope.launch {
                                musicRepository.recordPlay(songId)
                            }
                        }
                    }
                }
                delay(500)
            }
        }
    }
}
