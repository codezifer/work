package de.carsten.android.muzzic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import de.carsten.android.muzzic.ui.gradient1Color
import de.carsten.android.muzzic.ui.gradient2Color
import de.carsten.android.muzzic.ui.gradient3Color
import de.carsten.android.muzzic.viewmodel.PlayerViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun PlayerScreen(modifier: Modifier = Modifier) {
    val viewModel: PlayerViewModel = koinViewModel()
    val currentSong by viewModel.currentSong.observeAsState()
    val isPlaying by viewModel.isPlaying.observeAsState(false)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        gradient1Color,
                        gradient2Color,
                        gradient3Color
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            
        }
    }
}
