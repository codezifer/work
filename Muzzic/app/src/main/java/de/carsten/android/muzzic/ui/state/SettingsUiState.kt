package de.carsten.android.muzzic.ui.state

import de.carsten.android.muzzic.visualization.component.VisualizerEngine

data class SettingsUiState(
    val musicDirectory: String? = null,
    val playlistDirectory: String? = null,
    val visualizerEngine: VisualizerEngine = VisualizerEngine.BARS,
    val projectMPreset: String? = null,
    val availablePresets: List<String> = emptyList(),
    val barsShimmerEnabled: Boolean = true,
    val barsTipGlowEnabled: Boolean = true,
)
