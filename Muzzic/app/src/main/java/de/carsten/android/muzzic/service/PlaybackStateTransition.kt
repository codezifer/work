package de.carsten.android.muzzic.service

enum class PlaybackStateTransition {
    IDLE_IDLE,
    IDLE_BUFFERING,
    IDLE_READY,
    IDLE_PLAYING, // If it goes directly from IDLE to a playing state (less common without buffering)

    BUFFERING_BUFFERING,
    BUFFERING_READY,
    BUFFERING_PLAYING,
    BUFFERING_IDLE, // e.g., buffering failed, went back to idle

    READY_READY, // State is ready, but not necessarily playing/paused (e.g., seeking)
    READY_IDLE,    // Stopped from ready state
    PLAYING_PLAYING, // still playing
    PLAYING_PAUSED,
    PLAYING_ENDED,
    PLAYING_BUFFERING, // Rebuffering while playing

    PAUSED_PAUSED, // still paused
    PAUSED_PLAYING,
    PAUSED_BUFFERING, // Rebuffering while paused
    PAUSED_IDLE, // Stopped from paused state

    ENDED_ENDED,
    ENDED_BUFFERING, // Preparing next track after one ended
    ENDED_IDLE,      // Playlist ended, player is now idle
    ENDED_PLAYING,   // e.g. repeat current song

    UNKNOWN
}
