package de.carsten.android.muzzic.ui.player

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import de.carsten.android.muzzic.ui.AppNavHost
import de.carsten.android.muzzic.ui.screens.PlayerScreen
import de.carsten.android.muzzic.ui.screens.PlaylistsScreen
import de.carsten.android.muzzic.ui.screens.StatisticsScreen

@Composable
fun MusicPlayerApp() {
    var currentTab by remember { mutableIntStateOf(0) }
    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it },
            )
        },
    ) { paddingValues ->
        val paddingMod = Modifier.padding(paddingValues)
        when (currentTab) {
            0 -> PlayerScreen(paddingMod)
            1 -> AppNavHost(paddingMod)
            2 -> PlaylistsScreen(paddingMod)
            3 -> StatisticsScreen(paddingMod)
        }
    }
}
