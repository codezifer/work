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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.persistence.entity.Song
import de.carsten.android.muzzic.ui.gradient1Color
import de.carsten.android.muzzic.ui.gradient2Color
import de.carsten.android.muzzic.ui.gradient3Color
import de.carsten.android.muzzic.ui.screens.controls.VolumeControl
import de.carsten.android.muzzic.ui.theme.AppTheme
import de.carsten.android.muzzic.utils.extractAlbumArt
import de.carsten.android.muzzic.viewmodel.PlayerViewModel
import kotlinx.coroutines.flow.map
import org.koin.androidx.compose.koinViewModel

@Composable
fun PlayerScreen(
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = koinViewModel(),
) {
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.musicService.map { it?.isPlaying() ?: false }.collectAsState(false)
    val progress by viewModel.progress.collectAsState()
    val duration by viewModel.duration.collectAsState()

    PlayerScreenContent(
        modifier = modifier,
        currentSong = currentSong,
        isPlaying = isPlaying,
        progress = progress,
        duration = duration,
        onPrevClicked = viewModel::onPrevClicked,
        onNextClicked = viewModel::onNextClicked,
        onPlayPauseClicked = viewModel::togglePlayPause,
        onProgressChanged = viewModel::onProgressChanged,
    )
}

@Composable
fun PlayerScreenContent(
    modifier: Modifier,
    currentSong: Song?,
    isPlaying: Boolean,
    progress: Float,
    duration: Long,
    onPrevClicked: () -> Unit,
    onPlayPauseClicked: () -> Unit,
    onNextClicked: () -> Unit,
    onProgressChanged: (Float) -> Unit,
) {
    val appName = stringResource(R.string.app_name)
    val volume = stringResource(R.string.volume)

    AppTheme {
        Box(
            modifier =
                modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    gradient1Color,
                                    gradient2Color,
                                    gradient3Color,
                                ),
                        ),
                    ),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp),
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
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                        Text(
                            text = appName,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }

                // Context
                PlayerScreenContext(
                    albumArtPainter =
                        rememberAsyncImagePainter(
                            model =
                                ImageRequest
                                    .Builder(LocalContext.current)
                                    .data(
                                        extractAlbumArt(
                                            LocalContext.current,
                                            currentSong?.filePath ?: "",
                                        ),
                                    ).build(),
                        ),
                    songTitle = currentSong?.title,
                    artistName = currentSong?.artist,
                    isPlaying = isPlaying,
                    progress = progress,
                    duration = duration,
                    onPreviousClicked = onPrevClicked,
                    onPlayPauseClicked = onPlayPauseClicked,
                    onNextClicked = onNextClicked,
                    onProgressChanged = onProgressChanged,
                )
            }
        }
    }
}

@Composable
@Preview
@Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
fun PlayerScreenPreview() {
    PlayerScreenContent(
        Modifier.padding(2.dp),
        Song(
            title = "This is a test song",
            artist = "Test-Artist",
            album = "Test-Album",
        ),
        isPlaying = true,
        progress = 0.45f,
        duration = 225000L,
        onPrevClicked = {},
        onPlayPauseClicked = {},
        onNextClicked = {},
        onProgressChanged = { value -> },
    )
}
