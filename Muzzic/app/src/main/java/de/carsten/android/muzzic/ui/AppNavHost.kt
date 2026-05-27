package de.carsten.android.muzzic.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import de.carsten.android.muzzic.ui.AppDestinations.ALBUM_ARGUMENT
import de.carsten.android.muzzic.ui.AppDestinations.ALBUM_SONGS
import de.carsten.android.muzzic.ui.AppDestinations.ARTIST_ALBUMS
import de.carsten.android.muzzic.ui.AppDestinations.ARTIST_ARGUMENT
import de.carsten.android.muzzic.ui.AppDestinations.GENRES
import de.carsten.android.muzzic.ui.AppDestinations.GENRE_ARTISTS
import de.carsten.android.muzzic.ui.AppDestinations.LIBRARY
import de.carsten.android.muzzic.ui.AppDestinations.LIBRARY_GRAPH
import de.carsten.android.muzzic.ui.AppDestinations.PLAYER
import de.carsten.android.muzzic.ui.AppDestinations.PLAYLISTS
import de.carsten.android.muzzic.ui.AppDestinations.QUEUE
import de.carsten.android.muzzic.ui.AppDestinations.STATISTICS
import de.carsten.android.muzzic.ui.navigation.MusicAppState
import de.carsten.android.muzzic.ui.screens.AlbumSongsScreen
import de.carsten.android.muzzic.ui.screens.ArtistAlbumsScreen
import de.carsten.android.muzzic.ui.screens.GenresScreen
import de.carsten.android.muzzic.ui.screens.LibraryScreen
import de.carsten.android.muzzic.ui.screens.PlayerScreen
import de.carsten.android.muzzic.ui.screens.PlayingQueueScreen
import de.carsten.android.muzzic.ui.screens.PlaylistsScreen
import de.carsten.android.muzzic.ui.screens.StatisticsScreen
import de.carsten.android.muzzic.viewmodel.PlayingQueueViewModel
import de.carsten.android.muzzic.viewmodel.SelectionViewModel

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    appState: MusicAppState,
    startDestination: String = PLAYER,
    selectionViewModel: SelectionViewModel,
    playingQueueViewModel: PlayingQueueViewModel,
) {
    val navController = remember { appState.navController }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(PLAYER) {
            PlayerScreen(
                modifier = modifier,
                appState = appState,
            )
        }

        navigation(
            route = LIBRARY_GRAPH,
            startDestination = LIBRARY
        ) {
            composable(LIBRARY) {
                LibraryScreen(
                    modifier = modifier,
                    appState = appState,
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
                    appState = appState,
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
                    appState = appState,
                    onBackClick = { navController.popBackStack() },
                    selectionViewModel = selectionViewModel
                )
            }
        }

        composable(QUEUE) {
            PlayingQueueScreen(
                modifier = modifier,
                appState = appState,
                playingQueueViewModel = playingQueueViewModel,
                selectionViewModel = selectionViewModel
            )
        }

        composable(PLAYLISTS) {
            PlaylistsScreen(
                modifier = modifier,
                appState = appState,
                playingQueueViewModel = playingQueueViewModel,
                onPlaylistClick = {
                    navController.navigate(QUEUE) {
                        popUpTo(PLAYLISTS) { inclusive = true }
                    }
                },
                onPlayPlaylist = {
                    navController.navigate(PLAYER) {
                        popUpTo(PLAYER) { inclusive = true }
                    }
                }
            )
        }

        composable(GENRES) {
            GenresScreen(
                modifier = modifier,
                appState = appState,
                onGenreClick = {
                    navController.navigate(GENRE_ARTISTS) {
                        popUpTo(LIBRARY) { inclusive = true }
                    }
                }
            )
        }

        composable(STATISTICS) {
            StatisticsScreen(
                modifier = modifier,
                appState = appState,
            )
        }
    }
}
