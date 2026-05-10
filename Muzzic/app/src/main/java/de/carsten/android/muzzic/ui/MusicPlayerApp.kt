package de.carsten.android.muzzic.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.carsten.android.muzzic.ui.screens.controls.BottomNavigationBar
import de.carsten.android.muzzic.ui.screens.controls.SelectionToolbar
import de.carsten.android.muzzic.ui.screens.controls.ToolbarMode
import de.carsten.android.muzzic.viewmodel.PlayingQueueViewModel
import de.carsten.android.muzzic.viewmodel.SelectionViewModel
import de.carsten.android.muzzic.viewmodel.states.SelectionState
import org.koin.androidx.compose.koinViewModel

@Composable
fun MusicPlayerApp(
    selectionViewModel: SelectionViewModel = koinViewModel(),
    queueViewModel: PlayingQueueViewModel = koinViewModel()
) {
    val navController: NavHostController = rememberNavController()
    val selectionState: SelectionState by selectionViewModel.selectionState.collectAsStateWithLifecycle()
    val navBackStackEntry: NavBackStackEntry? by navController.currentBackStackEntryAsState()
    val currentDestination: String? = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController)
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
            val showSelectionToolbar = selectionState.isActive || currentDestination == AppDestinations.QUEUE
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
                    onConfirm = {
                        selectionViewModel.confirmSelection {
                            navController.navigate(AppDestinations.QUEUE)
                        }
                    },
                    onCancel = { selectionViewModel.clearSelection() },
                    onClearQueue = { queueViewModel.clear() },
                    onPersistQueue = { queueViewModel.persistCurrentQueue() }
                )
            }
        }
    }
}
