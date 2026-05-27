package de.carsten.android.muzzic.service

import androidx.media3.common.Player
import de.carsten.android.muzzic.logging.logger
import de.carsten.android.muzzic.utils.playbackStateToString

/**
 * Manages and resolves playback state transitions for analytics or UI purposes.
 */
class PlaybackStateManager {
    private val logger = logger()
    var playbackStateTransition: PlaybackStateTransition = PlaybackStateTransition.UNKNOWN
        private set

    /**
     * Resolves the transition between two playback states.
     *
     * @param player The player instance to check for additional status (like isPlaying).
     * @param oldState The previous state of the player.
     * @param newState The current/new state of the player.
     * @param isPlaying A cached playing status to help resolve READY -> READY transitions.
     */
    fun resolveTransition(
        player: Player,
        oldState: Int,
        newState: Int,
        isPlaying: Boolean
    ): PlaybackStateTransition {
        playbackStateTransition = when (Pair(oldState, newState)) {
            Pair(Player.STATE_IDLE, Player.STATE_BUFFERING) -> PlaybackStateTransition.IDLE_BUFFERING
            Pair(Player.STATE_IDLE, Player.STATE_READY) -> {
                if (player.playWhenReady) PlaybackStateTransition.IDLE_PLAYING else PlaybackStateTransition.IDLE_READY
            }

            Pair(Player.STATE_BUFFERING, Player.STATE_READY) -> {
                if (player.playWhenReady && player.isPlaying) PlaybackStateTransition.BUFFERING_PLAYING
                else PlaybackStateTransition.BUFFERING_READY
            }

            Pair(Player.STATE_BUFFERING, Player.STATE_IDLE) -> PlaybackStateTransition.BUFFERING_IDLE
            Pair(Player.STATE_READY, Player.STATE_ENDED) -> PlaybackStateTransition.PLAYING_ENDED
            Pair(Player.STATE_READY, Player.STATE_BUFFERING) -> PlaybackStateTransition.PLAYING_BUFFERING
            Pair(Player.STATE_READY, Player.STATE_IDLE) -> PlaybackStateTransition.READY_IDLE
            Pair(Player.STATE_ENDED, Player.STATE_BUFFERING) -> PlaybackStateTransition.ENDED_BUFFERING
            Pair(Player.STATE_ENDED, Player.STATE_IDLE) -> PlaybackStateTransition.ENDED_IDLE

            Pair(Player.STATE_READY, Player.STATE_READY) -> {
                val isNowPlaying = player.isPlaying
                if (!isPlaying && isNowPlaying) PlaybackStateTransition.PAUSED_PLAYING
                else if (isPlaying && !isNowPlaying) PlaybackStateTransition.PLAYING_PAUSED
                else PlaybackStateTransition.READY_READY
            }

            else -> handleUnknownTransition(oldState, newState, player)
        }
        return playbackStateTransition
    }

    private fun handleUnknownTransition(oldState: Int, newState: Int, player: Player): PlaybackStateTransition {
        if (oldState != newState) {
            logger.warning("Unknown Transition: ${playbackStateToString(oldState)} -> ${playbackStateToString(newState)}")
            return PlaybackStateTransition.UNKNOWN
        }

        return when (newState) {
            Player.STATE_IDLE -> PlaybackStateTransition.IDLE_IDLE
            Player.STATE_BUFFERING -> PlaybackStateTransition.BUFFERING_BUFFERING
            Player.STATE_READY -> if (player.isPlaying) PlaybackStateTransition.PLAYING_PLAYING else PlaybackStateTransition.PAUSED_PAUSED
            Player.STATE_ENDED -> PlaybackStateTransition.ENDED_ENDED
            else -> PlaybackStateTransition.UNKNOWN
        }
    }
}
