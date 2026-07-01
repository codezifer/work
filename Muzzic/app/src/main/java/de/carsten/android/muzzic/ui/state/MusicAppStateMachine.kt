package de.carsten.android.muzzic.ui.state

import de.carsten.android.muzzic.ui.AppDestinations
import de.carsten.android.muzzic.ui.navigation.NavigationEvent
import de.carsten.android.muzzic.viewmodel.states.SelectionState

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
    fun reduce(currentState: AppUiState, event: NavigationEvent, selectionState: SelectionState = SelectionState()): Pair<AppUiState, List<AppSideEffect>> {
        val sideEffects = mutableListOf<AppSideEffect>()

        val newState =
            when (event) {
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

                NavigationEvent.ToSettings -> {
                    sideEffects.add(AppSideEffect.Navigate(event))
                    AppUiState.Settings
                }

                NavigationEvent.ToGenres -> {
                    sideEffects.add(AppSideEffect.Navigate(event))
                    AppUiState.Library(selectionActive = selectionState.isActive)
                }

                NavigationEvent.Back -> {
                    sideEffects.add(AppSideEffect.Navigate(event))
                    // Note: Real state after back depends on NavController backstack,
                    // but for our toolbar logic, we derive it from the route.
                    currentState
                }

                is NavigationEvent.ToArtistAlbums, is NavigationEvent.ToAlbumSongs, is NavigationEvent.ToGenreArtists -> {
                    sideEffects.add(AppSideEffect.Navigate(event))
                    AppUiState.Library(selectionActive = selectionState.isActive)
                }
            }

        return newState to sideEffects
    }

    /**
     * Maps a navigation route string to an [AppUiState].
     */
    fun fromRoute(route: String?, selectionActive: Boolean): AppUiState = when (route) {
        AppDestinations.PLAYER -> {
            AppUiState.Player
        }

        AppDestinations.LIBRARY,
        AppDestinations.LIBRARY_GRAPH,
        AppDestinations.ARTIST_ALBUMS,
        AppDestinations.ALBUM_SONGS,
        AppDestinations.GENRES,
        AppDestinations.GENRE_ARTISTS,
        -> {
            AppUiState.Library(selectionActive)
        }

        AppDestinations.QUEUE -> {
            AppUiState.Queue(isManaging = true)
        }

        AppDestinations.PLAYLISTS -> {
            AppUiState.Playlists
        }

        AppDestinations.STATISTICS -> {
            AppUiState.Statistics
        }

        AppDestinations.SETTINGS -> {
            AppUiState.Settings
        }

        else -> {
            AppUiState.Player
        }
    }
}
