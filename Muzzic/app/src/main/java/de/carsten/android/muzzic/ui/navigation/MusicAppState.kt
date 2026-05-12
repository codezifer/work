package de.carsten.android.muzzic.ui.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.carsten.android.muzzic.ui.AppDestinations
import de.carsten.android.muzzic.ui.state.AppUiState
import de.carsten.android.muzzic.ui.state.MusicAppStateMachine
import de.carsten.android.muzzic.viewmodel.states.SelectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * State holder for the [MusicPlayerApp] composable.
 * This class manages the navigation state, snackbar state, and other UI-related state.
 *
 * @property navController The [NavHostController] used for navigation.
 * @property snackbarHostState The [SnackbarHostState] for displaying notifications.
 * @property coroutineScope The [CoroutineScope] for launching side effects.
 * @property navigator The [AppNavigator] for handling navigation events.
 * @property stateMachine The [MusicAppStateMachine] for handling state transitions.
 */
@Stable
class MusicAppState(
    val navController: NavHostController,
    val snackbarHostState: SnackbarHostState,
    val coroutineScope: CoroutineScope,
    val navigator: AppNavigator,
    val stateMachine: MusicAppStateMachine = MusicAppStateMachine()
) {
    /**
     * Gets the current navigation route.
     */
    val currentRoute: String?
        @Composable get() = navController.currentBackStackEntryAsState().value?.destination?.route

    /**
     * Derives the current UI state based on the navigation route and selection state.
     */
    @Composable
    fun getUiState(selectionState: SelectionState): AppUiState {
        return stateMachine.fromRoute(currentRoute, selectionState.isActive)
    }

    /**
     * Processes a navigation event through the state machine.
     */
    fun onNavigationEvent(event: NavigationEvent, selectionState: SelectionState) {
        val (_, sideEffects) = stateMachine.reduce(
            currentState = AppUiState.Player, // Temp, will be refined if needed
            event = event,
            selectionState = selectionState
        )

        sideEffects.forEach { effect ->
            // In a more advanced version, we would handle all side effects here
            // For now, we delegate the navigation back to the navigator
            if (effect is de.carsten.android.muzzic.ui.state.AppSideEffect.Navigate) {
                navigator.navigate(effect.event)
            }
        }
    }

    /**
     * Shows a snackbar message.
     *
     * @param message The message to display.
     */
    fun showSnackbar(message: String) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }
}

/**
 * Remembers and creates an instance of [MusicAppState].
 *
 * @param navController The [NavHostController].
 * @param snackbarHostState The [SnackbarHostState].
 * @param coroutineScope The [CoroutineScope].
 * @return A remembered instance of [MusicAppState].
 */
@Composable
fun rememberMusicAppState(
    navController: NavHostController = rememberNavController(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): MusicAppState {
    val navigator = remember(navController) { AppNavigator(navController) }
    return remember(navController, snackbarHostState, coroutineScope, navigator) {
        MusicAppState(navController, snackbarHostState, coroutineScope, navigator)
    }
}
