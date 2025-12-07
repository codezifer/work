package de.carsten.android.muzzic.ui.player

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp

val tabs =
    listOf(
        "Player" to Icons.Default.PlayArrow,
        "Library" to Icons.Default.LibraryMusic,
        "Playlists" to Icons.AutoMirrored.Filled.PlaylistPlay,
        "Statistics" to Icons.Default.BarChart,
    )

@Composable
fun BottomNavigationBar(
    currentTab: Int,
    onTabSelected: (Int) -> Unit,
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.primaryContainer) {
        tabs.forEachIndexed { idx, (label, icon) ->
            NavigationBarItem(
                selected = currentTab == idx,
                onClick = { onTabSelected(idx) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label, fontSize = 10.sp) },
                colors =
                    NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                    ),
            )
        }
    }
}

@Preview
@Preview(name = "DarkMode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun BottomNavigationBarPreview() {
    BottomNavigationBar(1) { tab -> }
}
