package de.carsten.android.muzzic.ui

import android.annotation.SuppressLint
import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.persistence.dao.PlayingQueueDao
import de.carsten.android.muzzic.persistence.databaseModule
import de.carsten.android.muzzic.persistence.repo.PlayingQueueRepository
import de.carsten.android.muzzic.persistence.repoModule
import de.carsten.android.muzzic.scanning.scanningModule
import de.carsten.android.muzzic.service.MediaLibraryManager
import de.carsten.android.muzzic.service.serviceModule
import de.carsten.android.muzzic.ui.component.PlayerBackground
import de.carsten.android.muzzic.ui.component.TextInputDialog
import de.carsten.android.muzzic.ui.model.ColorSource
import de.carsten.android.muzzic.ui.model.SongDto
import de.carsten.android.muzzic.ui.navigation.MusicAppState
import de.carsten.android.muzzic.ui.navigation.NavigationEvent
import de.carsten.android.muzzic.ui.navigation.rememberMusicAppState
import de.carsten.android.muzzic.ui.screens.cards.rememberPaletteState
import de.carsten.android.muzzic.ui.screens.controls.BottomNavItem
import de.carsten.android.muzzic.ui.screens.controls.BottomNavigationBar
import de.carsten.android.muzzic.ui.screens.controls.SelectionToolbar
import de.carsten.android.muzzic.ui.screens.controls.ToolbarMode
import de.carsten.android.muzzic.ui.state.AppUiState
import de.carsten.android.muzzic.ui.theme.AppTheme
import de.carsten.android.muzzic.ui.theme.CustomColors
import de.carsten.android.muzzic.ui.utils.adjustForTheme
import de.carsten.android.muzzic.ui.utils.ensureContrast
import de.carsten.android.muzzic.ui.utils.extractColors
import de.carsten.android.muzzic.viewmodel.LibraryViewModel
import de.carsten.android.muzzic.viewmodel.PlayerViewModel
import de.carsten.android.muzzic.viewmodel.PlayingQueueViewModel
import de.carsten.android.muzzic.viewmodel.PlaylistViewModel
import de.carsten.android.muzzic.viewmodel.SelectionViewModel
import de.carsten.android.muzzic.viewmodel.SettingsViewModel
import de.carsten.android.muzzic.viewmodel.states.SelectionState
import de.carsten.android.muzzic.viewmodel.viewModelModule
import de.carsten.android.muzzic.visualization.service.VisualizerSink
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.KoinApplicationPreview
import org.koin.dsl.module

/**
 * Main entry point for the Music Player application UI.
 * Orchestrates the overall layout, navigation, and state-dependent components like toolbars.
 */
@Composable
fun MusicPlayerAppUI(
    libraryViewModel: LibraryViewModel = koinViewModel(),
    selectionViewModel: SelectionViewModel = koinViewModel(),
    playingQueueViewModel: PlayingQueueViewModel = koinViewModel(),
    playerViewModel: PlayerViewModel = koinViewModel(),
    playlistViewModel: PlaylistViewModel = koinViewModel(),
    settingsViewModel: SettingsViewModel = koinViewModel(),
    appState: MusicAppState = rememberMusicAppState(),
    startDestination: String = AppDestinations.PLAYER,
) {
    val selectionState: SelectionState by selectionViewModel.selectionState.collectAsStateWithLifecycle()
    val appUiState = appState.getUiState(selectionState)

    val playerUiState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val fastScrollMode by settingsViewModel.fastScrollMode.collectAsStateWithLifecycle()
    val currentSong = playerUiState.currentSong
    val palette by rememberPaletteState(currentSong?.albumArt)
    val isDark = isSystemInDarkTheme()
    val albumArtPath = currentSong?.albumArt

    val defaultBackground = MaterialTheme.colorScheme.primary
    val defaultContent = MaterialTheme.colorScheme.onPrimary
    val neutralColor = CustomColors.neutral
    val onNeutralColor = CustomColors.onNeutral
    val colorSource = remember(palette, isDark, defaultBackground, defaultContent) {
        val paletteColors = palette.extractColors(
            defaultBackground = defaultBackground,
            defaultContent = defaultContent,
        )
        val accentColor = paletteColors.backgroundColor.adjustForTheme(isDark)
        val contentColor = paletteColors.contentColor.ensureContrast(accentColor)
        ColorSource(
            accentColor = accentColor,
            contentColor = contentColor,
            labelColor = contentColor.copy(alpha = 0.8f),
            neutralColor = neutralColor,
            onNeutralColor = onNeutralColor,
        )
    }

    var showSavePlaylistDialog by remember { mutableStateOf(false) }

    val playlistSavedFormat = stringResource(R.string.snackbar_playlist_saved_format)
    val removedFromQueueMessage = stringResource(R.string.snackbar_removed_from_queue)
    val addToQueueLabel = stringResource(R.string.add_to_queue)
    val removeFromQueueLabel = stringResource(R.string.remove_from_queue)

    if (showSavePlaylistDialog) {
        TextInputDialog(
            title = stringResource(R.string.playlist_save),
            label = stringResource(R.string.playlist_name),
            onConfirm = { name ->
                playlistViewModel.persistCurrentQueueAsPlaylist(name)
                showSavePlaylistDialog = false
                appState.showSnackbar(playlistSavedFormat.format(name))
            },
            onDismiss = { showSavePlaylistDialog = false },
        )
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(appState.snackbarHostState) { data ->
                Snackbar(
                    modifier = Modifier
                        .padding(SPACING_NORMAL)
                        .padding(bottom = SNACKBAR_BOTTOM_PADDING),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SPACING_NORMAL),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = data.visuals.message,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        },
        containerColor = Color.Transparent,
    ) { paddingValues ->
        Box(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            PlayerBackground(
                albumArtPath = currentSong?.albumArt,
                blurRadius = BLUR_RADIUS_DEFAULT,
            )

            AppNavHost(
                modifier = Modifier.fillMaxSize(),
                appState = appState,
                colorSource = colorSource,
                fastScrollMode = fastScrollMode,
                albumArtPath = albumArtPath,
                startDestination = startDestination,
                libraryViewModel = libraryViewModel,
                selectionViewModel = selectionViewModel,
                playingQueueViewModel = playingQueueViewModel,
                playerViewModel = playerViewModel,
            )

            val bottomFadeColor = lerp(CustomColors.gradient3, Color.Black, BOTTOM_FADE_OVERLAY_ALPHA)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(SCREEN_CONTENT_BOTTOM_PADDING * 2)
                    .drawBehind {
                        drawRect(
                            brush =
                            Brush.verticalGradient(
                                colors = listOf(bottomFadeColor.copy(alpha = 0f), bottomFadeColor),
                                startY = 0f,
                                endY = size.height,
                            ),
                        )
                    },
            )

            BottomNavigationBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(APP_FLOATING_MARGIN),
                navController = appState.navController,
                colorSource = colorSource,
                clickHandlers = mapOf(
                    BottomNavItem.Player.route to {
                        appState.onNavigationEvent(NavigationEvent.ToPlayer, selectionState)
                    },
                    BottomNavItem.Library.route to {
                        selectionViewModel.clearSelection()
                        appState.onNavigationEvent(NavigationEvent.ToLibrary, selectionState)
                    },
                    BottomNavItem.Queue.route to {
                        appState.onNavigationEvent(NavigationEvent.ToQueue, selectionState)
                    },
                    BottomNavItem.Playlists.route to {
                        appState.onNavigationEvent(NavigationEvent.ToPlaylists, selectionState)
                    },
                    BottomNavItem.Statistics.route to {
                        appState.onNavigationEvent(NavigationEvent.ToStatistics, selectionState)
                    },
                ),
            )

            // Contextual Floating Toolbar powered by State Machine logic
            val showSelectionToolbar by remember(appUiState) {
                derivedStateOf {
                    when (appUiState) {
                        is AppUiState.Library -> appUiState.selectionActive
                        is AppUiState.Playlists -> appUiState.selectionActive
                        is AppUiState.Queue -> true
                        else -> false
                    }
                }
            }

            AnimatedVisibility(
                visible = showSelectionToolbar,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = UI_OVERLAY_OFFSET),
            ) {
                SelectionToolbar(
                    mode = if (appUiState is AppUiState.Queue) ToolbarMode.QUEUE_MGMT else ToolbarMode.SELECTION,
                    selectedCount = selectionState.selectedSongs.size,
                    colorSource = colorSource,
                    confirmIcon = if (appUiState is AppUiState.Queue) Icons.Default.Delete else Icons.Default.Add,
                    confirmLabel = if (appUiState is AppUiState.Queue) removeFromQueueLabel else addToQueueLabel,
                    onConfirm = remember(appUiState, removedFromQueueMessage) {
                        {
                            if (appUiState is AppUiState.Queue) {
                                selectionViewModel.confirmRemoval {
                                    appState.showSnackbar(removedFromQueueMessage)
                                }
                            } else {
                                playingQueueViewModel.setPlayQueueName(PLAYING_QUEUE)
                                selectionViewModel.confirmSelection { info ->
                                    appState.showSnackbar(info)
                                }
                            }
                        }
                    },
                    onCancel = remember {
                        {
                            selectionViewModel.rollbackEnqueued { info ->
                                appState.showSnackbar(info)
                            }
                        }
                    },
                    onClearQueue = remember {
                        { info ->
                            playingQueueViewModel.clear()
                            playingQueueViewModel.setPlayQueueName(PLAYING_QUEUE)
                            appState.showSnackbar(info)
                        }
                    },
                    onPersistQueue = remember {
                        { info ->
                            playingQueueViewModel.persistCurrentQueue()
                            appState.showSnackbar(info)
                        }
                    },
                    onSaveAsPlaylist = remember {
                        {
                            showSavePlaylistDialog = true
                        }
                    },
                )
            }
        }
    }
}

private class PreviewPlayingQueueRepository(playingQueueDao: PlayingQueueDao, private val songs: List<SongDto>) : PlayingQueueRepository(playingQueueDao) {
    override fun observePlayingQueue(): Flow<List<SongDto>> = flowOf(songs)
}

private val previewQueueSongs =
    listOf(
        SongDto(
            id = "preview-1",
            title = "Puritania",
            artist = "Dimmu Borgir",
            album = "Puritanical Euphoric Misantropia",
            trackNumber = 5,
            totalTracks = 12,
            duration = 180_000L,
            genre = "Black Metal",
            queuePosition = 0,
        ),
        SongDto(
            id = "preview-2",
            title = "Nights in White Satin",
            artist = "The Moody Blues",
            album = "Every Good Boy Deserves Favour",
            trackNumber = 4,
            totalTracks = 11,
            duration = 298_000L,
            genre = "Progressive Rock",
            queuePosition = 1,
        ),
        SongDto(
            id = "preview-3",
            title = "Hysteria",
            artist = "Muse",
            album = "Absolution",
            trackNumber = 5,
            totalTracks = 13,
            duration = 229_000L,
            genre = "Alternative Rock",
            queuePosition = 2,
        ),
        SongDto(
            id = "preview-4",
            title = "Clair de Lune",
            artist = "Claude Debussy",
            album = "Suite Bergamasque",
            trackNumber = 1,
            totalTracks = 4,
            duration = 300_000L,
            genre = "Classical",
            queuePosition = 3,
        ),
        SongDto(
            id = "preview-5",
            title = "Seven Nation Army",
            artist = "The White Stripes",
            album = "Elephant",
            trackNumber = 2,
            totalTracks = 16,
            duration = 232_000L,
            genre = "Garage Rock",
            queuePosition = 4,
        ),
    )

@SuppressLint("UnsafeOptInUsageError")
@Composable
private fun PreviewMusicPlayerApp(startDestination: String) {
    val context = LocalContext.current

    val previewApplication =
        object : Application() {
            init {
                attachBaseContext(context)
            }
        }
    val previewServiceModule =
        module {
            single<Application> { previewApplication }
            single<MediaLibraryManager> { MediaLibraryManager(context, connectToService = false) }
            single<VisualizerSink> { VisualizerSink(provideSpectrum = false) }
            single<PlayingQueueRepository> { PreviewPlayingQueueRepository(get(), previewQueueSongs) }
        }
    KoinApplicationPreview(
        application = {
            androidContext(context.applicationContext)
            modules(
                databaseModule,
                repoModule,
                scanningModule,
                serviceModule,
                previewServiceModule,
                viewModelModule,
                uiModule,
            )
        },
    ) {
        AppTheme {
            MusicPlayerAppUI(startDestination = startDestination)
        }
    }
}

@Composable
@Preview(showBackground = true, name = "Player · Light")
@Preview(showBackground = true, uiMode = PREVIEW_DARK_MODE, name = "Player · Dark")
fun MusicPlayerAppUIPreview() {
    PreviewMusicPlayerApp(startDestination = AppDestinations.PLAYER)
}

@Composable
@Preview(showBackground = true, name = "Queue · Light")
@Preview(showBackground = true, uiMode = PREVIEW_DARK_MODE, name = "Queue · Dark")
fun MusicPlayerAppUIQueuePreview() {
    PreviewMusicPlayerApp(startDestination = AppDestinations.QUEUE)
}

@Composable
@Preview(showBackground = true, name = "Library · Light")
@Preview(showBackground = true, uiMode = PREVIEW_DARK_MODE, name = "Library · Dark")
fun MusicPlayerAppUILibraryPreview() {
    PreviewMusicPlayerApp(startDestination = AppDestinations.LIBRARY_GRAPH)
}

@Composable
@Preview(showBackground = true, name = "Playlists · Light")
@Preview(showBackground = true, uiMode = PREVIEW_DARK_MODE, name = "Playlists · Dark")
fun MusicPlayerAppUIPlaylistsPreview() {
    PreviewMusicPlayerApp(startDestination = AppDestinations.PLAYLISTS_GRAPH)
}

@Composable
@Preview(showBackground = true, name = "Genres · Light")
@Preview(showBackground = true, uiMode = PREVIEW_DARK_MODE, name = "Genres · Dark")
fun MusicPlayerAppUIGenresPreview() {
    PreviewMusicPlayerApp(startDestination = AppDestinations.GENRES)
}

@Composable
@Preview(showBackground = true, name = "Statistics · Light")
@Preview(showBackground = true, uiMode = PREVIEW_DARK_MODE, name = "Statistics · Dark")
fun MusicPlayerAppUIStatisticsPreview() {
    PreviewMusicPlayerApp(startDestination = AppDestinations.STATISTICS)
}

@Composable
@Preview(showBackground = true, name = "Settings · Light")
@Preview(showBackground = true, uiMode = PREVIEW_DARK_MODE, name = "Settings · Dark")
fun MusicPlayerAppUISettingsPreview() {
    PreviewMusicPlayerApp(startDestination = AppDestinations.SETTINGS)
}
