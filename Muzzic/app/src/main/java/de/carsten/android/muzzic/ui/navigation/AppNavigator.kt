package de.carsten.android.muzzic.ui.navigation

import androidx.navigation.NavHostController
import de.carsten.android.muzzic.ui.AppDestinations

/**
 * Handles the navigation logic for the application by interacting with the [NavHostController].
 * This class abstracts the navigation routes and provides a clean API for the UI.
 *
 * @property navController The [NavHostController] used for navigation.
 */
class AppNavigator(private val navController: NavHostController) {
    /**
     * Processes a [NavigationEvent] and performs the corresponding navigation action.
     *
     * @param event The navigation event to handle.
     */
    fun navigate(event: NavigationEvent) {
        when (event) {
            NavigationEvent.ToPlayer -> {
                navigateToRoot(AppDestinations.PLAYER)
            }

            NavigationEvent.ToLibrary -> {
                navigateToRoot(AppDestinations.LIBRARY_GRAPH)
            }

            NavigationEvent.ToQueue -> {
                navigateToRoot(AppDestinations.QUEUE)
            }

            NavigationEvent.ToPlaylists -> {
                navigateToRoot(AppDestinations.PLAYLISTS)
            }

            NavigationEvent.ToStatistics -> {
                navigateToRoot(AppDestinations.STATISTICS)
            }

            NavigationEvent.ToGenres -> {
                navigateToRoot(AppDestinations.GENRES)
            }

            NavigationEvent.Back -> {
                navController.popBackStack()
            }

            is NavigationEvent.ToArtistAlbums -> {
                navController.navigate(AppDestinations.artistAlbums(event.artistName))
            }

            is NavigationEvent.ToAlbumSongs -> {
                navController.navigate(AppDestinations.albumSongs(event.artistName, event.albumName))
            }

            is NavigationEvent.ToGenreArtists -> {
                navController.navigate(AppDestinations.genreArtists(event.genreName))
            }
        }
    }

    /**
     * Navigates to a root destination in the app, clearing the backstack if necessary
     * to avoid multiple instances of the same destination.
     *
     * @param route The destination route.
     */
    private fun navigateToRoot(route: String) {
        navController.navigate(route) {
            // Pop up to the start destination of the graph to
            // avoid building up a large stack of destinations
            // on the back stack as users select items
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
            // Avoid multiple copies of the same destination when
            // reselecting the same item
            launchSingleTop = true
            // Restore state when reselecting a previously selected item
            restoreState = true
        }
    }
}
