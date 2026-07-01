package de.carsten.android.muzzic.ui.state

/**
 * Represents the high-level UI states of the application.
 * This is the "State" in our State Machine.
 */
sealed class AppUiState {
    data object Player : AppUiState()

    data class Library(val selectionActive: Boolean = false) : AppUiState()

    data class Queue(val isManaging: Boolean = true) : AppUiState()

    data object Playlists : AppUiState()

    data object Statistics : AppUiState()

    data object Settings : AppUiState()
}
