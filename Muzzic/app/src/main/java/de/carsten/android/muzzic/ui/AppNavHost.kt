package de.carsten.android.muzzic.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import de.carsten.android.muzzic.ui.AppDestinations.ALBUM_ARGUMENT
import de.carsten.android.muzzic.ui.AppDestinations.ALBUM_SONGS
import de.carsten.android.muzzic.ui.AppDestinations.ARTIST_ALBUMS
import de.carsten.android.muzzic.ui.AppDestinations.ARTIST_ARGUMENT
import de.carsten.android.muzzic.ui.AppDestinations.LIBRARY
import de.carsten.android.muzzic.ui.AppDestinations.LIBRARY_GRAPH
import de.carsten.android.muzzic.ui.AppDestinations.PLAYER
import de.carsten.android.muzzic.ui.AppDestinations.PLAYLISTS
import de.carsten.android.muzzic.ui.AppDestinations.QUEUE
import de.carsten.android.muzzic.ui.AppDestinations.STATISTICS
import de.carsten.android.muzzic.ui.screens.AlbumSongsScreen
import de.carsten.android.muzzic.ui.screens.ArtistAlbumsScreen
import de.carsten.android.muzzic.ui.screens.LibraryScreen
import de.carsten.android.muzzic.ui.screens.PlayerScreen
import de.carsten.android.muzzic.ui.screens.PlayingQueueScreen
import de.carsten.android.muzzic.ui.screens.PlaylistsScreen
import de.carsten.android.muzzic.ui.screens.StatisticsScreen
import de.carsten.android.muzzic.viewmodel.SelectionViewModel

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = PLAYER,
    selectionViewModel: SelectionViewModel,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(PLAYER) {
            PlayerScreen(modifier = modifier)
        }

        navigation(
            route = LIBRARY_GRAPH,
            startDestination = LIBRARY
        ) {
            composable(LIBRARY) {
                LibraryScreen(
                    modifier = modifier,
                    onArtistClick = { artistName ->
                        navController.navigate(AppDestinations.artistAlbums(artistName))
                    },
                    onAlbumClick = { artistName, albumName ->
                        navController.navigate(AppDestinations.albumSongs(artistName, albumName))
                    },
                    selectionViewModel = selectionViewModel
                )
            }

            composable(
                route = ARTIST_ALBUMS,
                arguments = listOf(navArgument(ARTIST_ARGUMENT) { type = NavType.StringType })
            ) {
                ArtistAlbumsScreen(
                    modifier = modifier,
                    onAlbumClick = { artistName, albumName ->
                        navController.navigate(AppDestinations.albumSongs(artistName, albumName))
                    },
                    onBackClick = { navController.popBackStack() },
                    selectionViewModel = selectionViewModel
                )
            }

            composable(
                route = ALBUM_SONGS,
                arguments = listOf(
                    navArgument(ARTIST_ARGUMENT) { type = NavType.StringType },
                    navArgument(ALBUM_ARGUMENT) { type = NavType.StringType }
                )
            ) {
                AlbumSongsScreen(
                    modifier = modifier,
                    onBackClick = { navController.popBackStack() },
                    selectionViewModel = selectionViewModel
                )
            }
        }

        composable(QUEUE) {
            PlayingQueueScreen(modifier = modifier)
        }

        composable(PLAYLISTS) {
            PlaylistsScreen(modifier = modifier)
        }

        composable(STATISTICS) {
            StatisticsScreen(modifier = modifier)
        }
    }
}
