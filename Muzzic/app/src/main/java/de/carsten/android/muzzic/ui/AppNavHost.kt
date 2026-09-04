package de.carsten.android.muzzic.ui

import androidx.compose.foundation.layout.padding
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
import de.carsten.android.muzzic.ui.AppDestinations.GENRE_ARGUMENT
import de.carsten.android.muzzic.ui.AppDestinations.GENRE_ARTISTS
import de.carsten.android.muzzic.ui.AppDestinations.LIBRARY
import de.carsten.android.muzzic.ui.AppDestinations.LIBRARY_GRAPH
import de.carsten.android.muzzic.ui.AppDestinations.PLAYER
import de.carsten.android.muzzic.ui.AppDestinations.PLAYLISTS
import de.carsten.android.muzzic.ui.AppDestinations.QUEUE
import de.carsten.android.muzzic.ui.AppDestinations.SETTINGS
import de.carsten.android.muzzic.ui.AppDestinations.STATISTICS
import de.carsten.android.muzzic.ui.model.ColorSource
import de.carsten.android.muzzic.ui.model.GenreDto
import de.carsten.android.muzzic.ui.model.PlaylistDto
import de.carsten.android.muzzic.ui.navigation.MusicAppState
import de.carsten.android.muzzic.ui.navigation.NavigationEvent
import de.carsten.android.muzzic.ui.screens.AlbumSongsScreen
import de.carsten.android.muzzic.ui.screens.ArtistAlbumsScreen
import de.carsten.android.muzzic.ui.screens.GenreArtistsScreen
import de.carsten.android.muzzic.ui.screens.GenresScreen
import de.carsten.android.muzzic.ui.screens.LibraryScreen
import de.carsten.android.muzzic.ui.screens.PlayerScreen
import de.carsten.android.muzzic.ui.screens.PlayingQueueScreen
import de.carsten.android.muzzic.ui.screens.PlaylistsScreen
import de.carsten.android.muzzic.ui.screens.SettingsScreen
import de.carsten.android.muzzic.ui.screens.StatisticsScreen
import de.carsten.android.muzzic.viewmodel.LibraryViewModel
import de.carsten.android.muzzic.viewmodel.PlayingQueueViewModel
import de.carsten.android.muzzic.viewmodel.SelectionViewModel

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    appState: MusicAppState,
    colorSource: ColorSource,
    albumArtPath: String? = null,
    startDestination: String = PLAYER,
    libraryViewModel: LibraryViewModel,
    selectionViewModel: SelectionViewModel,
    playingQueueViewModel: PlayingQueueViewModel,
) {
    val navController = remember { appState.navController }

    val onArtistClick = remember(appState, selectionViewModel) {
        { artistName: String ->
            appState.onNavigationEvent(
                NavigationEvent.ToArtistAlbums(artistName),
                selectionViewModel.selectionState.value,
            )
        }
    }

    val onAlbumClick = remember(appState, selectionViewModel) {
        { artistName: String, albumName: String ->
            appState.onNavigationEvent(
                NavigationEvent.ToAlbumSongs(artistName, albumName),
                selectionViewModel.selectionState.value,
            )
        }
    }

    val onPlaylistClick = remember(libraryViewModel, playingQueueViewModel, appState) {
        { playlistDto: PlaylistDto ->
            libraryViewModel.setIntoPlayingQueue(playlistDto.playlistId)
            playingQueueViewModel.setPlayQueueName(playlistDto.playlistName)
            appState.onNavigationEvent(NavigationEvent.ToQueue)
            appState.showSnackbar("Set ${playlistDto.playlistName}")
        }
    }

    val onPlayPlaylist = remember(libraryViewModel, playingQueueViewModel) {
        { playlistDto: PlaylistDto ->
            libraryViewModel.playPlaylist(playlistDto.playlistId)
            playingQueueViewModel.setPlayQueueName(playlistDto.playlistName)
            appState.onNavigationEvent(NavigationEvent.ToPlayer)
            appState.showSnackbar("Play playlist ${playlistDto.playlistName}")
        }
    }

    val onDeletePlaylist = remember(libraryViewModel) {
        { playlistDto: PlaylistDto ->
            libraryViewModel.deletePlaylist(playlistDto.playlistId)
        }
    }

    val onGenreClick = remember(appState, selectionViewModel) {
        { genreDto: GenreDto ->
            appState.onNavigationEvent(
                NavigationEvent.ToGenreArtists(genreDto.genreName),
                selectionViewModel.selectionState.value,
            )
        }
    }

    val onBackClick = remember(appState) {
        {
            appState.onNavigationEvent(NavigationEvent.Back)
        }
    }

    val onSettingsClick = remember(appState) {
        {
            appState.onNavigationEvent(NavigationEvent.ToSettings)
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier.padding(bottom = SCREEN_CONTENT_BOTTOM_PADDING),
    ) {
        composable(PLAYER) {
            PlayerScreen(
                modifier = modifier,
                appState = appState,
                colorSource = colorSource,
            )
        }

        navigation(
            route = LIBRARY_GRAPH,
            startDestination = LIBRARY,
        ) {
            composable(LIBRARY) {
                LibraryScreen(
                    modifier = modifier,
                    appState = appState,
                    colorSource = colorSource,
                    onArtistClick = onArtistClick,
                    onAlbumClick = onAlbumClick,
                    onGenreClick = onGenreClick,
                    onPlaylistClick = onPlaylistClick,
                    onSettingsClick = onSettingsClick,
                    libraryViewModel = libraryViewModel,
                    selectionViewModel = selectionViewModel,
                )
            }

            composable(
                route = ARTIST_ALBUMS,
                arguments = listOf(navArgument(ARTIST_ARGUMENT) { type = NavType.StringType }),
            ) {
                ArtistAlbumsScreen(
                    modifier = modifier,
                    appState = appState,
                    colorSource = colorSource,
                    onAlbumClick = onAlbumClick,
                    onBackClick = onBackClick,
                    selectionViewModel = selectionViewModel,
                )
            }

            composable(
                route = ALBUM_SONGS,
                arguments =
                listOf(
                    navArgument(ARTIST_ARGUMENT) { type = NavType.StringType },
                    navArgument(ALBUM_ARGUMENT) { type = NavType.StringType },
                ),
            ) {
                AlbumSongsScreen(
                    modifier = modifier,
                    appState = appState,
                    onBackClick = onBackClick,
                    selectionViewModel = selectionViewModel,
                )
            }

            composable(
                route = GENRE_ARTISTS,
                arguments = listOf(navArgument(GENRE_ARGUMENT) { type = NavType.StringType }),
            ) {
                GenreArtistsScreen(
                    modifier = modifier,
                    appState = appState,
                    colorSource = colorSource,
                    onArtistClick = onArtistClick,
                    onBackClick = onBackClick,
                )
            }
        }

        composable(QUEUE) {
            PlayingQueueScreen(
                modifier = modifier,
                appState = appState,
                playingQueueViewModel = playingQueueViewModel,
                selectionViewModel = selectionViewModel,
                colorSource = colorSource,
            )
        }

        composable(PLAYLISTS) {
            PlaylistsScreen(
                modifier = modifier,
                appState = appState,
                playingQueueViewModel = playingQueueViewModel,
                colorSource = colorSource,
                onPlaylistClick = onPlaylistClick,
                onPlayPlaylist = onPlayPlaylist,
                onDeletePlaylist = onDeletePlaylist,
            )
        }

        composable(GENRES) {
            GenresScreen(
                modifier = modifier,
                appState = appState,
                onGenreClick = onGenreClick,
            )
        }

        composable(STATISTICS) {
            StatisticsScreen(
                modifier = modifier,
                appState = appState,
            )
        }

        composable(SETTINGS) {
            SettingsScreen(
                modifier = modifier,
                appState = appState,
                onBackClick = onBackClick,
            )
        }
    }
}
