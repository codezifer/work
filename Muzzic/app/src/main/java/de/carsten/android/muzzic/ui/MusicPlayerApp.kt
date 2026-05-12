package de.carsten.android.muzzic.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.carsten.android.muzzic.ui.screens.controls.BottomNavItem
import de.carsten.android.muzzic.ui.screens.controls.BottomNavigationBar
import de.carsten.android.muzzic.ui.screens.controls.SelectionToolbar
import de.carsten.android.muzzic.ui.screens.controls.ToolbarMode
import de.carsten.android.muzzic.viewmodel.PlayerViewModel
import de.carsten.android.muzzic.viewmodel.PlayingQueueViewModel
import de.carsten.android.muzzic.viewmodel.SelectionViewModel
import de.carsten.android.muzzic.viewmodel.states.SelectionState
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun MusicPlayerApp(
    selectionViewModel: SelectionViewModel = koinViewModel(),
    queueViewModel: PlayingQueueViewModel = koinViewModel(),
    playerViewModel: PlayerViewModel = koinViewModel()
) {
    val navController: NavHostController = rememberNavController()
    val selectionState: SelectionState by selectionViewModel.selectionState.collectAsStateWithLifecycle()
    val navBackStackEntry: NavBackStackEntry? by navController.currentBackStackEntryAsState()
    val currentDestination: String? = navBackStackEntry?.destination?.route
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    modifier = Modifier
                        .border(4.dp, MaterialTheme.colorScheme.secondary)
                        .padding(12.dp)
                ) {
                    Text(data.visuals.message)
                }
            }
        },
        bottomBar = {
            BottomNavigationBar(
                navController = navController,
                clickHandlers = HashMap<String, () -> Unit>().apply {
                    this[BottomNavItem.Player.label] = {
                        playerViewModel.loadPlaylist(queueViewModel.currentPlayingQueue.value)
                    }
                    this[BottomNavItem.Library.label] = {
                        selectionViewModel.clearSelection()
                    }
                    this[BottomNavItem.Queue.label] = {
                        if (selectionState.isActive) {
                            selectionViewModel.confirmSelection {
                                selectionViewModel.toggleEnqueued()
                            }
                        } else {
                            queueViewModel.loadPlayingQueue()
                        }
                    }
                    this[BottomNavItem.Playlists.label] = {}
                    this[BottomNavItem.Statistics.label] = {}
                }
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
                navController = navController,
                selectionViewModel = selectionViewModel,
                modifier = Modifier.fillMaxSize()
            )

            // Floating Contextual Toolbar
            val isInQueue = currentDestination == AppDestinations.QUEUE
            val showSelectionToolbar = selectionState.isActive || isInQueue
            AnimatedVisibility(
                visible = showSelectionToolbar,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                SelectionToolbar(
                    mode = if (selectionState.isActive) ToolbarMode.SELECTION else ToolbarMode.QUEUE_MGMT,
                    selectedCount = selectionState.selectedSongs.size,
                    confirmIcon = if (isInQueue) Icons.Default.Delete else Icons.Default.Add,
                    confirmLabel = if (isInQueue) "Remove from Queue" else "Add to Queue",
                    onConfirm = { info ->
                        if (isInQueue) {
                            selectionViewModel.confirmRemoval {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Removed from Queue")
                                }
                                queueViewModel.loadPlayingQueue()
                            }
                        } else {
                            selectionViewModel.confirmSelection {
                                navController.navigate(AppDestinations.QUEUE)
                                selectionViewModel.toggleEnqueued()
                                scope.launch {
                                    snackbarHostState.showSnackbar(info)
                                }
                                queueViewModel.loadPlayingQueue()
                            }
                        }
                    },
                    onCancel = { info ->
                        selectionViewModel.clearSelection()
                        scope.launch {
                            snackbarHostState.showSnackbar(info)
                        }
                    },
                    onClearQueue = { info ->
                        queueViewModel.clear()
                        queueViewModel.loadPlayingQueue()
                        scope.launch {
                            snackbarHostState.showSnackbar(info)
                        }
                    },
                    onPersistQueue = { info ->
                        queueViewModel.persistCurrentQueue()
                        scope.launch {
                            snackbarHostState.showSnackbar(info)
                        }
                    }
                )
            }
        }
    }
}
