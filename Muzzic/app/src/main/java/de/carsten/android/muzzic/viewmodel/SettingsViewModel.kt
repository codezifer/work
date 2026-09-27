package de.carsten.android.muzzic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.carsten.android.muzzic.persistence.repo.AppSettingsRepository
import de.carsten.android.muzzic.persistence.repo.MusicRepository
import de.carsten.android.muzzic.ui.state.SettingsUiState
import de.carsten.android.muzzic.visualization.component.VisualizerColorSource
import de.carsten.android.muzzic.visualization.component.VisualizerEngine
import de.carsten.android.muzzic.visualization.projectm.PresetManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Bundled visualizer options for settings observation.
 */
private data class VisualizerOptions(
    val engine: VisualizerEngine,
    val preset: String?,
    val shimmerEnabled: Boolean,
    val tipGlowEnabled: Boolean,
    val colorSource: VisualizerColorSource,
)

/**
 * ViewModel for the Settings screen.
 * Manages app-wide configuration and manual library tasks.
 */
class SettingsViewModel(private val appSettingsRepository: AppSettingsRepository, private val musicRepository: MusicRepository, application: Application) :
    AndroidViewModel(application),
    UiStateViewModel<SettingsUiState> {

    override val uiState: StateFlow<SettingsUiState> = combine(
        appSettingsRepository.observeMusicDirectory(),
        appSettingsRepository.observePlaylistDirectory(),
        combine(
            appSettingsRepository.observeVisualizerEngine(),
            appSettingsRepository.observeProjectMPreset(),
            appSettingsRepository.observeBarsShimmerEnabled(),
            appSettingsRepository.observeBarsTipGlowEnabled(),
            appSettingsRepository.observeVisualizerColorSource(),
        ) { visualizerEngine, projectMPreset, barsShimmerEnabled, barsTipGlowEnabled, colorSource ->
            VisualizerOptions(visualizerEngine, projectMPreset, barsShimmerEnabled, barsTipGlowEnabled, colorSource)
        },
    ) { musicDir, playlistDir, visualizer ->
        val availablePresets = PresetManager.getAvailablePresets(getApplication())
        SettingsUiState(
            musicDirectory = musicDir,
            playlistDirectory = playlistDir,
            visualizerEngine = visualizer.engine,
            projectMPreset = visualizer.preset ?: availablePresets.firstOrNull(),
            availablePresets = availablePresets,
            barsShimmerEnabled = visualizer.shimmerEnabled,
            barsTipGlowEnabled = visualizer.tipGlowEnabled,
            visualizerColorSource = visualizer.colorSource,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    /**
     * Updates the active visualizer engine setting.
     */
    fun updateVisualizerEngine(engine: VisualizerEngine) {
        viewModelScope.launch {
            appSettingsRepository.saveVisualizerEngine(engine)
        }
    }

    /**
     * Updates the selected ProjectM preset name.
     */
    fun updateProjectMPreset(presetName: String) {
        viewModelScope.launch {
            appSettingsRepository.saveProjectMPreset(presetName)
        }
    }

    /**
     * Updates the mirrored-BARS shimmer effect toggle.
     */
    fun updateBarsShimmerEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appSettingsRepository.saveBarsShimmerEnabled(enabled)
        }
    }

    /**
     * Updates the mirrored-BARS tip-glow effect toggle.
     */
    fun updateBarsTipGlowEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appSettingsRepository.saveBarsTipGlowEnabled(enabled)
        }
    }

    /**
     * Updates the visualizer color source setting.
     */
    fun updateVisualizerColorSource(colorSource: VisualizerColorSource) {
        viewModelScope.launch {
            appSettingsRepository.saveVisualizerColorSource(colorSource)
        }
    }

    /**
     * Updates the music directory path.
     */
    fun updateMusicDirectory(path: String) {
        viewModelScope.launch {
            appSettingsRepository.saveMusicDirectory(path)
        }
    }

    /**
     * Updates the playlist directory path.
     */
    fun updatePlaylistDirectory(path: String) {
        viewModelScope.launch {
            appSettingsRepository.savePlaylistDirectory(path)
        }
    }

    /**
     * Triggers a manual music library scan.
     */
    fun scanMusicLibrary() {
        viewModelScope.launch {
            musicRepository.scanMusicLibrary()
        }
    }

    /**
     * Triggers a manual playlist import.
     */
    fun importPlaylists() {
        viewModelScope.launch {
            musicRepository.importPlaylists()
        }
    }

    /**
     * Clears the entire library (songs, playlists, statistics and queue).
     */
    fun clearLibrary() {
        viewModelScope.launch {
            musicRepository.clearLibrary()
        }
    }
}
