package de.carsten.android.muzzic.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import de.carsten.android.muzzic.ui.screens.AppNavHost
import de.carsten.android.muzzic.ui.screens.PlayerScreen
import de.carsten.android.muzzic.ui.screens.PlayingQueueScreen
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
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentTab) {
                0 -> PlayerScreen(Modifier.fillMaxSize())
                1 -> AppNavHost(Modifier.fillMaxSize())
                2 -> PlayingQueueScreen(Modifier.fillMaxSize())
                3 -> PlaylistsScreen(Modifier.fillMaxSize())
                4 -> StatisticsScreen(Modifier.fillMaxSize())
            }
        }
    }
}
