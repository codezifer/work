package de.carsten.android.muzzic.ui.state

import androidx.media3.common.Player
import de.carsten.android.muzzic.ui.model.SongDto
import de.carsten.android.muzzic.visualization.component.VisualizerEngine

data class PlayerUiState(
    val isConnected: Boolean = false,
    val currentSong: SongDto? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val progress: Float = 0f,
    val shuffleModeEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val amplitudes: List<Float> = emptyList(),
    val visualizerEngine: VisualizerEngine = VisualizerEngine.BARS,
    val projectMPreset: String? = null,
)
