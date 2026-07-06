package de.carsten.android.muzzic.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.media3.common.Player
import de.carsten.android.muzzic.AppConfig
import de.carsten.android.muzzic.UI_EMPTY
import de.carsten.android.muzzic.ui.FONT_SIZE_CAPTION
import de.carsten.android.muzzic.ui.FONT_SIZE_LARGE_TITLE
import de.carsten.android.muzzic.ui.FONT_SIZE_SUBTITLE
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.SPACING_MEDIUM
import de.carsten.android.muzzic.ui.component.IndicatorSlider
import de.carsten.android.muzzic.ui.model.AlbumArtInput
import de.carsten.android.muzzic.ui.model.ColorSource
import de.carsten.android.muzzic.ui.model.composableColorSource
import de.carsten.android.muzzic.ui.screens.controls.AlbumArtControl
import de.carsten.android.muzzic.ui.screens.controls.PlayerControls
import de.carsten.android.muzzic.ui.screens.controls.VolumeControl
import de.carsten.android.muzzic.ui.utils.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreenContext(
    albumArtPath: String?,
    songTitle: String?,
    artistName: String?,
    isPlaying: Boolean,
    progress: Float, // Value between 0f and 1f
    duration: Long,
    shuffleModeEnabled: Boolean = false,
    repeatMode: Int = Player.REPEAT_MODE_OFF,
    amplitudes: List<Float> = emptyList(),
    colorSource: ColorSource = composableColorSource(),
    onPlayPauseClicked: () -> Unit = {},
    onNextClicked: () -> Unit = {},
    onPreviousClicked: () -> Unit = {},
    onProgressChanged: (Float) -> Unit = {}, // Callback for when user scrubs the progress bar
    onToggleShuffle: () -> Unit = {},
    onToggleRepeat: () -> Unit = {},
) {
    val textColor = MaterialTheme.colorScheme.onSurface

    // We don't need local scrubbing state anymore as IndicatorSlider handles it
    // But we need it for the duration text below the slider if we want those to update too
    // Let's keep a simplified version or just accept that those labels only update on finish
    // User asked for "numerische Position" to be displayed, which we do in the bubble.

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        // Album Cover
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = albumArtPath,
                transitionSpec = {
                    fadeIn(
                        animationSpec =
                        tween(AppConfig.Ui.ALBUM_ART_FADE_IN_OUT),
                    ).togetherWith(
                        fadeOut(
                            animationSpec =
                            tween(AppConfig.Ui.ALBUM_ART_FADE_IN_OUT),
                        ),
                    )
                },
                label = "AlbumArtTransition",
            ) { targetPath ->
                AlbumArtControl(
                    albumArtInput = if (targetPath == null) {
                        AlbumArtInput.None
                    } else {
                        AlbumArtInput.FromPath(targetPath)
                    },
                )
            }
        }

        // Song Info
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = songTitle ?: UI_EMPTY,
                color = textColor,
                fontSize = FONT_SIZE_LARGE_TITLE,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = artistName ?: UI_EMPTY,
                color = textColor.copy(alpha = 0.7f),
                fontSize = FONT_SIZE_SUBTITLE,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Progress Bar
        Column(modifier = Modifier.fillMaxWidth()) {
            IndicatorSlider(
                value = progress,
                onValueChange = { /* handled by indicator internal state */ },
                onValueChangeFinished = { newVal ->
                    onProgressChanged(newVal)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = colorSource.accentColor,
                    activeTrackColor = colorSource.accentColor,
                    inactiveTrackColor = textColor.copy(alpha = 0.3f),
                ),
                indicatorFormatter = { valPos -> formatDuration((duration * valPos).toLong()) },
                indicatorColor = colorSource.accentColor,
            )
            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SPACING_MEDIUM),
                // Align with slider padding
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formatDuration((duration * progress).toLong()),
                    color = textColor.copy(alpha = 0.8f),
                    fontSize = FONT_SIZE_CAPTION,
                )
                Text(
                    text = formatDuration((duration - (duration * progress)).toLong()),
                    color = textColor.copy(alpha = 0.8f),
                    fontSize = FONT_SIZE_CAPTION,
                )
            }
        }

        // Controls
        PlayerControls(
            isPlaying = isPlaying,
            shuffleModeEnabled = shuffleModeEnabled,
            repeatMode = repeatMode,
            amplitudes = amplitudes,
            colorSource = colorSource,
            onPlayPauseClicked = onPlayPauseClicked,
            onNextClicked = onNextClicked,
            onPreviousClicked = onPreviousClicked,
            onToggleShuffle = onToggleShuffle,
            onToggleRepeat = onToggleRepeat,
        )

        // volume
        VolumeControl(accentColor = colorSource.accentColor)
    }
}

@Composable
@Preview
@Preview(uiMode = PREVIEW_DARK_MODE, name = "PlayerScreenPreview_Playing_Dark")
fun PlayerScreenPreview_Playing() {
    PlayerScreenContext(
        albumArtPath = null,
        songTitle = "The Greatest Show",
        artistName = "Panic! At The Disco",
        isPlaying = true,
        progress = 0.45f,
        duration = 225000,
        shuffleModeEnabled = true,
        repeatMode = Player.REPEAT_MODE_ALL,
        amplitudes = List(16) { it.toFloat() / 16f },
    )
}

@Composable
@Preview
@Preview(uiMode = PREVIEW_DARK_MODE, name = "PlayerScreenPreview_Paused_Dark")
fun PlayerScreenPreview_Paused() {
    PlayerScreenContext(
        albumArtPath = null,
        songTitle = "Bohemian Rhapsody (Remastered 2011)",
        artistName = "Queen",
        isPlaying = false,
        progress = 0.15f,
        duration = 225000,
        shuffleModeEnabled = false,
        repeatMode = Player.REPEAT_MODE_OFF,
        amplitudes = List(16) { 0f },
    )
}
