package de.carsten.android.muzzic.ui.player

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
import androidx.navigation.compose.rememberNavController
import de.carsten.android.muzzic.ui.AppDestinations
import de.carsten.android.muzzic.ui.screens.AppNavHost
import de.carsten.android.muzzic.ui.screens.SelectionToolbar
import de.carsten.android.muzzic.viewmodel.SelectionViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun MusicPlayerApp(
    viewModel: SelectionViewModel = koinViewModel()
) {
    val navController = rememberNavController()
    val selectionState by viewModel.selectionState.collectAsStateWithLifecycle()

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
                selectionViewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )

            // Floating Selection Toolbar
            AnimatedVisibility(
                visible = selectionState.isActive,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                SelectionToolbar(
                    selectedCount = selectionState.selectedArtists.size + selectionState.selectedAlbums.size + selectionState.selectedSongs.size,
                    onConfirm = {
                        viewModel.confirmSelection {
                            navController.navigate(AppDestinations.QUEUE)
                        }
                    },
                    onCancel = { viewModel.clearSelection() }
                )
            }
        }
    }
}
