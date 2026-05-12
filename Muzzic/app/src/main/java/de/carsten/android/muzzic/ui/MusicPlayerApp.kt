package de.carsten.android.muzzic.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.carsten.android.muzzic.ui.navigation.MusicAppState
import de.carsten.android.muzzic.ui.navigation.NavigationEvent
import de.carsten.android.muzzic.ui.navigation.rememberMusicAppState
import de.carsten.android.muzzic.ui.screens.controls.BottomNavItem
import de.carsten.android.muzzic.ui.screens.controls.BottomNavigationBar
import de.carsten.android.muzzic.ui.screens.controls.SelectionToolbar
import de.carsten.android.muzzic.ui.screens.controls.ToolbarMode
import de.carsten.android.muzzic.ui.state.AppUiState
import de.carsten.android.muzzic.viewmodel.PlayerViewModel
import de.carsten.android.muzzic.viewmodel.PlayingQueueViewModel
import de.carsten.android.muzzic.viewmodel.SelectionViewModel
import de.carsten.android.muzzic.viewmodel.states.SelectionState
import org.koin.androidx.compose.koinViewModel

/**
 * Main entry point for the Music Player application UI.
 * Orchestrates the overall layout, navigation, and state-dependent components like toolbars.
 */
@Composable
fun MusicPlayerApp(
    selectionViewModel: SelectionViewModel = koinViewModel(),
    queueViewModel: PlayingQueueViewModel = koinViewModel(),
    playerViewModel: PlayerViewModel = koinViewModel(),
    appState: MusicAppState = rememberMusicAppState()
) {
    val selectionState: SelectionState by selectionViewModel.selectionState.collectAsStateWithLifecycle()
    val uiState = appState.getUiState(selectionState)

    Scaffold(
        snackbarHost = {
            SnackbarHost(appState.snackbarHostState) { data ->
                Snackbar(
                    modifier = Modifier.padding(12.dp),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = data.visuals.message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        bottomBar = {
            BottomNavigationBar(
                navController = appState.navController,
                clickHandlers = mapOf(
                    BottomNavItem.Player.label to {
                        appState.onNavigationEvent(NavigationEvent.ToPlayer, selectionState)
                    },
                    BottomNavItem.Library.label to {
                        selectionViewModel.clearSelection()
                        appState.onNavigationEvent(NavigationEvent.ToLibrary, selectionState)
                    },
                    BottomNavItem.Queue.label to {
                        queueViewModel.loadPlayingQueue()
                        appState.onNavigationEvent(NavigationEvent.ToQueue, selectionState)
                    },
                    BottomNavItem.Playlists.label to {
                        appState.onNavigationEvent(NavigationEvent.ToPlaylists, selectionState)
                    },
                    BottomNavItem.Statistics.label to {
                        appState.onNavigationEvent(NavigationEvent.ToStatistics, selectionState)
                    }
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AppNavHost(
                navController = appState.navController,
                selectionViewModel = selectionViewModel,
                modifier = Modifier.fillMaxSize()
            )

            // Contextual Floating Toolbar powered by State Machine logic
            val showSelectionToolbar = when (uiState) {
                is AppUiState.Library -> uiState.selectionActive
                is AppUiState.Queue -> true
                else -> false
            }

            AnimatedVisibility(
                visible = showSelectionToolbar,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                SelectionToolbar(
                    mode = if (uiState is AppUiState.Queue) ToolbarMode.QUEUE_MGMT else ToolbarMode.SELECTION,
                    selectedCount = selectionState.selectedSongs.size,
                    confirmIcon = if (uiState is AppUiState.Queue) Icons.Default.Delete else Icons.Default.Add,
                    confirmLabel = if (uiState is AppUiState.Queue) "Remove from Queue" else "Add to Queue",
                    onConfirm = {
                        if (uiState is AppUiState.Queue) {
                            selectionViewModel.confirmRemoval {
                                appState.showSnackbar("Removed from Queue")
                                queueViewModel.loadPlayingQueue()
                            }
                        } else {
                            selectionViewModel.confirmSelection { info ->
                                appState.showSnackbar(info)
                                queueViewModel.loadPlayingQueue()
                            }
                        }
                    },
                    onCancel = {
                        selectionViewModel.rollbackEnqueued { info ->
                            appState.showSnackbar(info)
                            queueViewModel.loadPlayingQueue()
                        }
                    },
                    onClearQueue = {
                        queueViewModel.clear()
                        queueViewModel.loadPlayingQueue()
                        appState.showSnackbar("Queue cleared")
                    },
                    onPersistQueue = {
                        queueViewModel.persistCurrentQueue()
                        appState.showSnackbar("Queue persisted")
                    }
                )
            }
        }
    }
}
