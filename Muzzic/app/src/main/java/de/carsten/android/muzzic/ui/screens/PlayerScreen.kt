package de.carsten.android.muzzic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.ui.FONT_SIZE_TITLE
import de.carsten.android.muzzic.ui.GLASS_CONTAINER_ALPHA
import de.carsten.android.muzzic.ui.GLASS_PANEL_CORNER_RADIUS
import de.carsten.android.muzzic.ui.ICON_SIZE_MEDIUM
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.SPACING_LARGE
import de.carsten.android.muzzic.ui.SPACING_MEDIUM
import de.carsten.android.muzzic.ui.SPACING_TINY
import de.carsten.android.muzzic.ui.model.ColorSource
import de.carsten.android.muzzic.ui.model.SongDto
import de.carsten.android.muzzic.ui.model.composableColorSource
import de.carsten.android.muzzic.ui.navigation.MusicAppState
import de.carsten.android.muzzic.ui.theme.AppTheme
import de.carsten.android.muzzic.viewmodel.PlayerViewModel
import de.carsten.android.muzzic.visualization.audio.SpectrumProcessor
import de.carsten.android.muzzic.visualization.bus.SpectrumBus
import de.carsten.android.muzzic.visualization.component.VisualizerColorSource
import de.carsten.android.muzzic.visualization.component.VisualizerEngine
import org.koin.androidx.compose.koinViewModel

private val GLASS_CONTAINER_ROUNDING = GLASS_PANEL_CORNER_RADIUS

@Composable
fun PlayerScreen(modifier: Modifier = Modifier, appState: MusicAppState, colorSource: ColorSource, playerViewModel: PlayerViewModel = koinViewModel()) {
    val uiState by playerViewModel.uiState.collectAsStateWithLifecycle()

    PlayerScreenContent(
        modifier = modifier,
        currentSong = uiState.currentSong,
        isPlaying = uiState.isPlaying,
        progress = uiState.progress,
        duration = uiState.duration,
        shuffleModeEnabled = uiState.shuffleModeEnabled,
        repeatMode = uiState.repeatMode,
        visualizerEngine = uiState.visualizerEngine,
        projectMPreset = uiState.projectMPreset,
        barsShimmerEnabled = uiState.barsShimmerEnabled,
        barsTipGlowEnabled = uiState.barsTipGlowEnabled,
        visualizerColorSource = uiState.visualizerColorSource,
        spectrumBus = playerViewModel.spectrumBus,
        spectrumProcessor = playerViewModel.spectrumProcessor,
        colorSource = colorSource,
        onPrevClicked = playerViewModel::onPrevClicked,
        onNextClicked = playerViewModel::onNextClicked,
        onPlayPauseClicked = playerViewModel::togglePlayPause,
        onProgressChanged = playerViewModel::onProgressChanged,
        onToggleShuffle = playerViewModel::toggleShuffle,
        onToggleRepeat = playerViewModel::toggleRepeatMode,
    )
}

@Composable
private fun PlayerScreenContent(
    modifier: Modifier,
    currentSong: SongDto?,
    isPlaying: Boolean,
    progress: Float,
    duration: Long,
    shuffleModeEnabled: Boolean,
    repeatMode: Int,
    visualizerEngine: VisualizerEngine = VisualizerEngine.BARS,
    projectMPreset: String? = null,
    barsShimmerEnabled: Boolean = true,
    barsTipGlowEnabled: Boolean = true,
    visualizerColorSource: VisualizerColorSource = VisualizerColorSource.ALBUM_ART,
    spectrumBus: SpectrumBus? = null,
    spectrumProcessor: SpectrumProcessor? = null,
    colorSource: ColorSource = composableColorSource(),
    onPrevClicked: () -> Unit,
    onPlayPauseClicked: () -> Unit,
    onNextClicked: () -> Unit,
    onProgressChanged: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
) {
    val appName = stringResource(R.string.app_name)

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        // Glass Container
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(SPACING_LARGE)
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = GLASS_CONTAINER_ALPHA),
                    shape = RoundedCornerShape(GLASS_CONTAINER_ROUNDING),
                )
                .padding(SPACING_LARGE),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = colorSource.accentColor,
                        modifier = Modifier.size(ICON_SIZE_MEDIUM),
                    )
                    Text(
                        text = appName,
                        color = colorSource.accentColor,
                        fontSize = FONT_SIZE_TITLE,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = SPACING_MEDIUM),
                    )
                }
            }

            // Context
            PlayerScreenContext(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                albumArtPath = currentSong?.albumArt,
                songTitle = currentSong?.title,
                artistName = currentSong?.artist,
                isPlaying = isPlaying,
                progress = progress,
                duration = duration,
                shuffleModeEnabled = shuffleModeEnabled,
                repeatMode = repeatMode,
                visualizerEngine = visualizerEngine,
                projectMPreset = projectMPreset,
                barsShimmerEnabled = barsShimmerEnabled,
                barsTipGlowEnabled = barsTipGlowEnabled,
                visualizerColorSource = visualizerColorSource,
                spectrumBus = spectrumBus,
                spectrumProcessor = spectrumProcessor,
                colorSource = colorSource,
                onPreviousClicked = onPrevClicked,
                onPlayPauseClicked = onPlayPauseClicked,
                onNextClicked = onNextClicked,
                onProgressChanged = onProgressChanged,
                onToggleShuffle = onToggleShuffle,
                onToggleRepeat = onToggleRepeat,
            )
        }
    }
}

@Composable
@Preview
@Preview(uiMode = PREVIEW_DARK_MODE)
fun PlayerScreenPreview() {
    AppTheme {
        PlayerScreenContent(
            Modifier.padding(SPACING_TINY),
            SongDto(
                title = "This is a test song",
                artist = "Test-Artist",
                album = "Test-Album",
            ),
            isPlaying = true,
            progress = 0.45f,
            duration = 225000L,
            shuffleModeEnabled = false,
            repeatMode = Player.REPEAT_MODE_OFF,
            colorSource = composableColorSource(),
            onPrevClicked = {},
            onPlayPauseClicked = {},
            onNextClicked = {},
            onProgressChanged = { value -> },
            onToggleShuffle = {},
            onToggleRepeat = {},
        )
    }
}
