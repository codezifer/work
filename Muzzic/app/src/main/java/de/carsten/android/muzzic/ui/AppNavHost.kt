package de.carsten.android.muzzic.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import de.carsten.android.muzzic.ui.AppDestinations.ALBUM_SONGS
import de.carsten.android.muzzic.ui.AppDestinations.ARTIST_ALBUMS
import de.carsten.android.muzzic.ui.AppDestinations.LIBRARY
import de.carsten.android.muzzic.ui.screens.AlbumSongsScreen
import de.carsten.android.muzzic.ui.screens.ArtistAlbumsScreen
import de.carsten.android.muzzic.ui.screens.LibraryScreen

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = LIBRARY,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(LIBRARY) {
            LibraryScreen(
                modifier = modifier,
                onArtistClick = { artistName ->
                    navController.navigate(AppDestinations.artistAlbums(artistName))
                },
                onAlbumClick = { artistName, albumName ->
                    navController.navigate(AppDestinations.albumSongs(artistName, albumName))
                }
            )
        }

        composable(
            route = ARTIST_ALBUMS,
            arguments = listOf(navArgument("artistName") { type = NavType.StringType })
        ) {
            ArtistAlbumsScreen(
                modifier = modifier,
                onAlbumClick = { artistName, albumName ->
                    navController.navigate(AppDestinations.albumSongs(artistName, albumName))
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = ALBUM_SONGS,
            arguments = listOf(
                navArgument("artistName") { type = NavType.StringType },
                navArgument("albumName") { type = NavType.StringType }
            )
        ) {
            AlbumSongsScreen(
                modifier = modifier,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
