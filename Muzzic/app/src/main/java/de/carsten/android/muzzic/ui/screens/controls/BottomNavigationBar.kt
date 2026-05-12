package de.carsten.android.muzzic.ui.screens.controls

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.carsten.android.muzzic.ui.AppDestinations
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.theme.AppTheme

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String, var onClick: () -> Unit = {}) {
    object Player : BottomNavItem(AppDestinations.PLAYER, Icons.Default.PlayArrow, "Player")
    object Library : BottomNavItem(AppDestinations.LIBRARY_GRAPH, Icons.Default.LibraryMusic, "Library")
    object Queue : BottomNavItem(AppDestinations.QUEUE, Icons.Default.Queue, "Queue")
    object Playlists : BottomNavItem(AppDestinations.PLAYLISTS, Icons.AutoMirrored.Filled.PlaylistPlay, "Playlists")
    object Statistics : BottomNavItem(AppDestinations.STATISTICS, Icons.Default.BarChart, "Statistics")
}

val bottomNavItems = listOf(
    BottomNavItem.Player,
    BottomNavItem.Library,
    BottomNavItem.Queue,
    BottomNavItem.Playlists,
    BottomNavItem.Statistics
)

@Composable
fun BottomNavigationBar(
    navController: NavController,
    clickHandlers: Map<String, () -> Unit> = HashMap()
) {
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry.value?.destination

    AppTheme {
        NavigationBar(containerColor = MaterialTheme.colorScheme.primaryContainer) {
            bottomNavItems.forEach { item ->
                val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        navController.navigate(item.route) {
                            // Pop up to the start destination of the graph to
                            // avoid building up a large stack of destinations
                            // on the back stack as users select items
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination when
                            // reselecting the same item
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                        clickHandlers[item.label]?.invoke()
                    },
                    icon = { Icon(item.icon, contentDescription = item.label) },
                    label = { Text(item.label, fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        indicatorColor = MaterialTheme.colorScheme.inversePrimary,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                    ),
                )
            }
        }
    }
}

@Preview
@Preview(uiMode = PREVIEW_DARK_MODE, name = "BottomNavigationBarPreview_Dark")
@Composable
fun BottomNavigationBarPreview() {
    BottomNavigationBar(
        rememberNavController(),
    )
}
