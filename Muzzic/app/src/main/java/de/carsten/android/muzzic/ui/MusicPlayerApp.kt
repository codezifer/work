package de.carsten.android.muzzic.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.model.toAlbumArtUri
import de.carsten.android.muzzic.ui.component.PlayerBackground
import de.carsten.android.muzzic.ui.component.TextInputDialog
import de.carsten.android.muzzic.ui.model.ColorSource
import de.carsten.android.muzzic.ui.navigation.MusicAppState
import de.carsten.android.muzzic.ui.navigation.NavigationEvent
import de.carsten.android.muzzic.ui.navigation.rememberMusicAppState
import de.carsten.android.muzzic.ui.screens.cards.rememberPaletteState
import de.carsten.android.muzzic.ui.screens.controls.BottomNavItem
import de.carsten.android.muzzic.ui.screens.controls.BottomNavigationBar
import de.carsten.android.muzzic.ui.screens.controls.SelectionToolbar
import de.carsten.android.muzzic.ui.screens.controls.ToolbarMode
import de.carsten.android.muzzic.ui.state.AppUiState
import de.carsten.android.muzzic.ui.theme.CustomColors
import de.carsten.android.muzzic.ui.utils.adjustForTheme
import de.carsten.android.muzzic.ui.utils.ensureContrast
import de.carsten.android.muzzic.ui.utils.extractColors
import de.carsten.android.muzzic.viewmodel.LibraryViewModel
import de.carsten.android.muzzic.viewmodel.PlayerViewModel
import de.carsten.android.muzzic.viewmodel.PlayingQueueViewModel
import de.carsten.android.muzzic.viewmodel.PlaylistViewModel
import de.carsten.android.muzzic.viewmodel.SelectionViewModel
import de.carsten.android.muzzic.viewmodel.states.SelectionState
import org.koin.androidx.compose.koinViewModel

/**
 * Main entry point for the Music Player application UI.
 * Orchestrates the overall layout, navigation, and state-dependent components like toolbars.
 */
@Composable
fun MusicPlayerApp(
    libraryViewModel: LibraryViewModel = koinViewModel(),
    selectionViewModel: SelectionViewModel = koinViewModel(),
    playingQueueViewModel: PlayingQueueViewModel = koinViewModel(),
    playerViewModel: PlayerViewModel = koinViewModel(),
    playlistViewModel: PlaylistViewModel = koinViewModel(),
    appState: MusicAppState = rememberMusicAppState(),
) {
    val selectionState: SelectionState by selectionViewModel.selectionState.collectAsStateWithLifecycle()
    val uiState = appState.getUiState(selectionState)

    val playerUiState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val currentSong = playerUiState.currentSong
    val palette by rememberPaletteState(currentSong?.albumArt?.toAlbumArtUri())
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

    if (showSavePlaylistDialog) {
        TextInputDialog(
            title = stringResource(R.string.playlist_save),
            label = stringResource(R.string.playlist_name),
            onConfirm = { name ->
                playlistViewModel.persistCurrentQueueAsPlaylist(name)
                showSavePlaylistDialog = false
                appState.showSnackbar("Playlist '$name' saved")
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
                albumArtPath = albumArtPath,
                libraryViewModel = libraryViewModel,
                selectionViewModel = selectionViewModel,
                playingQueueViewModel = playingQueueViewModel,
            )

            BottomNavigationBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(APP_FLOATING_MARGIN),
                navController = appState.navController,
                colorSource = colorSource,
                clickHandlers =
                mapOf(
                    BottomNavItem.Player.label to {
                        appState.onNavigationEvent(NavigationEvent.ToPlayer, selectionState)
                    },
                    BottomNavItem.Library.label to {
                        selectionViewModel.clearSelection()
                        appState.onNavigationEvent(NavigationEvent.ToLibrary, selectionState)
                    },
                    BottomNavItem.Queue.label to {
                        appState.onNavigationEvent(NavigationEvent.ToQueue, selectionState)
                    },
                    BottomNavItem.Playlists.label to {
                        appState.onNavigationEvent(NavigationEvent.ToPlaylists, selectionState)
                    },
                    BottomNavItem.Statistics.label to {
                        appState.onNavigationEvent(NavigationEvent.ToStatistics, selectionState)
                    },
                ),
            )

            // Contextual Floating Toolbar powered by State Machine logic
            val showSelectionToolbar by remember(uiState) {
                derivedStateOf {
                    when (uiState) {
                        is AppUiState.Library -> uiState.selectionActive
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
                    mode = if (uiState is AppUiState.Queue) ToolbarMode.QUEUE_MGMT else ToolbarMode.SELECTION,
                    selectedCount = selectionState.selectedSongs.size,
                    colorSource = colorSource,
                    confirmIcon = if (uiState is AppUiState.Queue) Icons.Default.Delete else Icons.Default.Add,
                    confirmLabel = if (uiState is AppUiState.Queue) "Remove from Queue" else "Add to Queue",
                    onConfirm = remember(uiState) {
                        {
                            if (uiState is AppUiState.Queue) {
                                selectionViewModel.confirmRemoval {
                                    appState.showSnackbar("Removed from Queue")
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
                        { info ->
                            showSavePlaylistDialog = true
                        }
                    },
                )
            }
        }
    }
}
