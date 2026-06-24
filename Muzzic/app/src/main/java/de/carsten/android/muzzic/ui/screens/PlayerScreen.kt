package de.carsten.android.muzzic.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import de.carsten.android.muzzic.ui.BLUR_RADIUS_DEFAULT
import de.carsten.android.muzzic.ui.FONT_SIZE_TITLE
import de.carsten.android.muzzic.ui.GLASS_PANEL_CORNER_RADIUS
import de.carsten.android.muzzic.ui.ICON_SIZE_MEDIUM
import de.carsten.android.muzzic.ui.SPACING_LARGE
import de.carsten.android.muzzic.ui.SPACING_MEDIUM
import de.carsten.android.muzzic.ui.SPACING_TINY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.persistence.entity.Song
import de.carsten.android.muzzic.ui.model.ColorSource
import de.carsten.android.muzzic.ui.navigation.MusicAppState
import de.carsten.android.muzzic.ui.theme.AppTheme
import de.carsten.android.muzzic.ui.theme.CustomColors
import de.carsten.android.muzzic.viewmodel.PlayerViewModel
import org.koin.androidx.compose.koinViewModel

private val DEFAULT_BACKGROUND_BLUR = BLUR_RADIUS_DEFAULT
private const val BACKGROUND_OVERLAY_ALPHA = 0.3f
private val GLASS_CONTAINER_ROUNDING = GLASS_PANEL_CORNER_RADIUS
private val GLASS_CONTAINER_ALPHA = 0.5f

@Composable
fun PlayerScreen(modifier: Modifier = Modifier, appState: MusicAppState, colorSource: ColorSource, playerViewModel: PlayerViewModel = koinViewModel()) {
    val currentSong by playerViewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()
    val progress by playerViewModel.progress.collectAsStateWithLifecycle()
    val duration by playerViewModel.duration.collectAsStateWithLifecycle()
    val shuffleModeEnabled by playerViewModel.shuffleModeEnabled.collectAsStateWithLifecycle()
    val repeatMode by playerViewModel.repeatMode.collectAsStateWithLifecycle()

    PlayerScreenContent(
        modifier = modifier,
        currentSong = currentSong,
        isPlaying = isPlaying,
        progress = progress,
        duration = duration,
        shuffleModeEnabled = shuffleModeEnabled,
        repeatMode = repeatMode,
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
fun PlayerScreenContent(
    modifier: Modifier,
    currentSong: Song?,
    isPlaying: Boolean,
    progress: Float,
    duration: Long,
    shuffleModeEnabled: Boolean,
    repeatMode: Int,
    colorSource: ColorSource = ColorSource(accentColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.inversePrimary),
    onPrevClicked: () -> Unit,
    onPlayPauseClicked: () -> Unit,
    onNextClicked: () -> Unit,
    onProgressChanged: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
) {
    val appName = stringResource(R.string.app_name)

    AppTheme {
        Box(
            modifier = modifier.fillMaxSize(),
        ) {
            // Blurred Background Layer
            PlayerBackground(
                albumArtPath = currentSong?.albumArt,
                blurRadius = DEFAULT_BACKGROUND_BLUR,
            )

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
                    albumArtPath = currentSong?.albumArt,
                    songTitle = currentSong?.title,
                    artistName = currentSong?.artist,
                    isPlaying = isPlaying,
                    progress = progress,
                    duration = duration,
                    shuffleModeEnabled = shuffleModeEnabled,
                    repeatMode = repeatMode,
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
}

/**
 * Renders a blurred background using the album art of the current song.
 *
 * @param albumArtPath The path to the album art image.
 * @param blurRadius The radius of the blur effect.
 */
@Composable
fun PlayerBackground(albumArtPath: String?, blurRadius: Dp) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Base Gradient (fallback if no image is available)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            CustomColors.gradient1,
                            CustomColors.gradient2,
                            CustomColors.gradient3,
                        ),
                    ),
                ),
        )

        // Blurred Album Art
        AnimatedContent(
            targetState = albumArtPath,
            transitionSpec = {
                fadeIn(animationSpec = tween(500)).togetherWith(fadeOut(animationSpec = tween(500)))
            },
            label = "PlayerBackgroundTransition",
        ) { targetPath ->
            if (targetPath != null) {
                AsyncImage(
                    model = targetPath,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(blurRadius),
                )
            } else {
                // Return an empty box if no album art is available
                Box(modifier = Modifier.fillMaxSize())
            }
        }

        // Dark Overlay to maintain contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = BACKGROUND_OVERLAY_ALPHA)),
        )
    }
}

@Composable
@Preview
@Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
fun PlayerScreenPreview() {
    PlayerScreenContent(
        Modifier.padding(SPACING_TINY),
        Song(
            title = "This is a test song",
            artist = "Test-Artist",
            album = "Test-Album",
        ),
        isPlaying = true,
        progress = 0.45f,
        duration = 225000L,
        shuffleModeEnabled = false,
        repeatMode = Player.REPEAT_MODE_OFF,
        onPrevClicked = {},
        onPlayPauseClicked = {},
        onNextClicked = {},
        onProgressChanged = { value -> },
        onToggleShuffle = {},
        onToggleRepeat = {},
    )
}
