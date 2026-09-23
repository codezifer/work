package de.carsten.android.muzzic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.carsten.android.muzzic.persistence.repo.AppSettingsRepository
import de.carsten.android.muzzic.persistence.repo.MusicRepository
import de.carsten.android.muzzic.ui.state.SettingsUiState
import de.carsten.android.muzzic.visualization.component.VisualizerEngine
import de.carsten.android.muzzic.visualization.projectm.PresetManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
        appSettingsRepository.observeVisualizerEngine(),
        appSettingsRepository.observeProjectMPreset(),
    ) { musicDir, playlistDir, visualizerEngine, projectMPreset ->
        val availablePresets = PresetManager.getAvailablePresets(getApplication())
        SettingsUiState(
            musicDirectory = musicDir,
            playlistDirectory = playlistDir,
            visualizerEngine = visualizerEngine,
            projectMPreset = projectMPreset ?: availablePresets.firstOrNull(),
            availablePresets = availablePresets,
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
