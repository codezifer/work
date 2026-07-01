package de.carsten.android.muzzic.ui.state

import de.carsten.android.muzzic.ui.navigation.NavigationEvent

/**
 * Represents side effects that should be triggered during transitions.
 */
sealed class AppSideEffect {
    data class Navigate(val event: NavigationEvent) : AppSideEffect()

    data class ShowSnackbar(val message: String) : AppSideEffect()

    data object LoadQueue : AppSideEffect()

    data object ClearSelection : AppSideEffect()
}
