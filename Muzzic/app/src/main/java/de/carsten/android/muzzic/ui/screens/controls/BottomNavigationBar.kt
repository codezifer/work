package de.carsten.android.muzzic.ui.screens.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.carsten.android.muzzic.ui.AppDestinations
import de.carsten.android.muzzic.ui.FONT_SIZE_SMALL
import de.carsten.android.muzzic.ui.GLASS_PANEL_CORNER_RADIUS
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.component.GlassPanel
import de.carsten.android.muzzic.ui.model.ColorSource
import de.carsten.android.muzzic.ui.theme.AppTheme

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String, var onClick: () -> Unit = {}) {
    object Player : BottomNavItem(AppDestinations.PLAYER, Icons.Default.PlayArrow, "Player")

    object Library : BottomNavItem(AppDestinations.LIBRARY_GRAPH, Icons.Default.LibraryMusic, "Library")

    object Queue : BottomNavItem(AppDestinations.QUEUE, Icons.Default.Queue, "Queue")

    object Playlists : BottomNavItem(AppDestinations.PLAYLISTS, Icons.AutoMirrored.Filled.PlaylistPlay, "Playlists")

    object Statistics : BottomNavItem(AppDestinations.STATISTICS, Icons.Default.BarChart, "Statistics")
}

val bottomNavItems =
    listOf(
        BottomNavItem.Player,
        BottomNavItem.Library,
        BottomNavItem.Queue,
        BottomNavItem.Playlists,
        BottomNavItem.Statistics,
    )

/**
 * A floating, glassy navigation bar.
 *
 * @param modifier Modifier for the container.
 * @param navController Controller to handle navigation.
 * @param colorSource Current colors from [ColorSource].
 * @param clickHandlers Optional map of click handlers for each item.
 */
@Composable
fun BottomNavigationBar(
    modifier: Modifier = Modifier,
    navController: NavController,
    colorSource: ColorSource = ColorSource(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer),
    clickHandlers: Map<String, () -> Unit> = HashMap(),
) {
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry.value?.destination

    GlassPanel(
        modifier = modifier,
        containerColor = colorSource.accentColor,
        alpha = 0.75f,
        shape = RoundedCornerShape(GLASS_PANEL_CORNER_RADIUS),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            bottomNavItems.forEach { item ->
                val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                        clickHandlers[item.label]?.invoke()
                    },
                    icon = { Icon(item.icon, contentDescription = item.label) },
                    label = { Text(item.label, fontSize = FONT_SIZE_SMALL) },
                    colors =
                    NavigationBarItemDefaults.colors(
                        selectedIconColor = colorSource.contentColor,
                        selectedTextColor = colorSource.contentColor,
                        indicatorColor = colorSource.contentColor.copy(alpha = 0.2f),
                        unselectedIconColor = colorSource.labelColor,
                        unselectedTextColor = colorSource.labelColor,
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
    AppTheme {
        BottomNavigationBar(
            navController = rememberNavController(),
        )
    }
}
