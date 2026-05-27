package de.carsten.android.muzzic.ui.state

import de.carsten.android.muzzic.ui.AppDestinations
import de.carsten.android.muzzic.ui.navigation.NavigationEvent
import de.carsten.android.muzzic.viewmodel.states.Selection
import de.carsten.android.muzzic.viewmodel.states.SelectionState

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
}

/**
 * Represents side effects that should be triggered during transitions.
 */
sealed class AppSideEffect {
    data class Navigate(val event: NavigationEvent) : AppSideEffect()
    data class ShowSnackbar(val message: String) : AppSideEffect()
    data object LoadQueue : AppSideEffect()
    data object ClearSelection : AppSideEffect()
}

/**
 * A lightweight State Machine that handles UI state transitions and side effects.
 * It provides a centralized, readable way to understand how navigation and
 * selection states interact.
 */
class MusicAppStateMachine {

    /**
     * Reduces the current state and an incoming event to a new state and a list of side effects.
     *
     * @param currentState The current UI state.
     * @param event The navigation or UI event.
     * @param selectionState The current selection state from the ViewModel.
     * @return A pair of the new [AppUiState] and a list of [AppSideEffect]s.
     */
    fun reduce(
        currentState: AppUiState,
        event: NavigationEvent,
        selectionState: SelectionState = SelectionState()
    ): Pair<AppUiState, List<AppSideEffect>> {
        val sideEffects = mutableListOf<AppSideEffect>()

        val newState = when (event) {
            NavigationEvent.ToPlayer -> {
                sideEffects.add(AppSideEffect.Navigate(event))
                AppUiState.Player
            }

            NavigationEvent.ToLibrary -> {
                sideEffects.add(AppSideEffect.Navigate(event))
                AppUiState.Library(selectionActive = selectionState.isActive)
            }

            NavigationEvent.ToQueue -> {
                sideEffects.add(AppSideEffect.Navigate(event))
                sideEffects.add(AppSideEffect.LoadQueue)
                AppUiState.Queue(isManaging = true)
            }

            NavigationEvent.ToPlaylists -> {
                sideEffects.add(AppSideEffect.Navigate(event))
                AppUiState.Playlists
            }

            NavigationEvent.ToStatistics -> {
                sideEffects.add(AppSideEffect.Navigate(event))
                AppUiState.Statistics
            }

            NavigationEvent.Back -> {
                sideEffects.add(AppSideEffect.Navigate(event))
                // Note: Real state after back depends on NavController backstack,
                // but for our toolbar logic, we derive it from the route.
                currentState
            }

            is NavigationEvent.ToArtistAlbums, is NavigationEvent.ToAlbumSongs -> {
                sideEffects.add(AppSideEffect.Navigate(event))
                AppUiState.Library(selectionActive = selectionState.isActive)
            }
        }

        return newState to sideEffects
    }

    /**
     * Maps a navigation route string to an [AppUiState].
     */
    fun fromRoute(route: String?, selectionActive: Boolean): AppUiState {
        return when (route) {
            AppDestinations.PLAYER -> AppUiState.Player
            AppDestinations.LIBRARY, AppDestinations.LIBRARY_GRAPH, AppDestinations.ARTIST_ALBUMS, AppDestinations.ALBUM_SONGS -> {
                AppUiState.Library(selectionActive)
            }
            AppDestinations.QUEUE -> AppUiState.Queue(isManaging = true)
            AppDestinations.PLAYLISTS -> AppUiState.Playlists
            AppDestinations.STATISTICS -> AppUiState.Statistics
            else -> AppUiState.Player
        }
    }
}
