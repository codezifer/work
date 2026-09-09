package de.carsten.android.muzzic.ui.state

import androidx.media3.common.MediaItem
import de.carsten.android.muzzic.ui.PLAYING_QUEUE
import de.carsten.android.muzzic.ui.model.PlayingQueueDto

data class PlayingQueueUiState(
    val queue: List<PlayingQueueDto> = emptyList(),
    val currentSong: MediaItem? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val name: String = PLAYING_QUEUE,
)
