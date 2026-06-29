package de.carsten.android.muzzic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.carsten.android.muzzic.persistence.repo.AppSettingsRepository
import de.carsten.android.muzzic.persistence.repo.MusicRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the Settings screen.
 * Manages app-wide configuration and manual library tasks.
 */
class SettingsViewModel(private val appSettingsRepository: AppSettingsRepository, private val musicRepository: MusicRepository) : ViewModel() {

    /**
     * State flow for the music directory path.
     */
    val musicDirectory: StateFlow<String?> = appSettingsRepository.observeMusicDirectory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * State flow for the playlist directory path.
     */
    val playlistDirectory: StateFlow<String?> = appSettingsRepository.observePlaylistDirectory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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
}
