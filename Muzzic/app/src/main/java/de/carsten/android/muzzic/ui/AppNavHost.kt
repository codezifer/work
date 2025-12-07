package de.carsten.android.muzzic.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.carsten.android.muzzic.ui.AppDestinations.LIBRARY
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
            LibraryScreen(modifier)
        }
    }
}
