package de.carsten.android.muzzic.ui.screens.controls

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.ui.CORNER_RADIUS_FULL
import de.carsten.android.muzzic.ui.ELEVATION_MEDIUM
import de.carsten.android.muzzic.ui.ICON_SIZE_MEDIUM
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.VOLUME_BAR_HEIGHT
import de.carsten.android.muzzic.ui.VOLUME_BAR_WIDTH
import de.carsten.android.muzzic.ui.VOLUME_PREVIEW_WIDTH
import de.carsten.android.muzzic.ui.component.IndicatorSlider
import de.carsten.android.muzzic.ui.shape.TailDirection
import de.carsten.android.muzzic.ui.theme.AppTheme
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolumeControl(modifier: Modifier = Modifier, accentColor: Color = MaterialTheme.colorScheme.primary) {
    AppTheme {
        val context = LocalContext.current
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Aktuellen und maximalen Lautstärkewert initial auslesen
        val initialVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

        // Den Zustand des Sliders verwalten
        var volume by remember { mutableIntStateOf(initialVolume) }
        var showSlider by remember { mutableStateOf(false) }

        // BroadcastReceiver, um auf externe Lautstärkeänderungen zu reagieren
        // (z.B. durch die Hardware-Tasten am Gerät)
        DisposableEffect(Unit) {
            val receiver =
                object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        if (intent.action == "android.media.VOLUME_CHANGED_ACTION") {
                            val newVolume = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", initialVolume)
                            volume = newVolume
                        }
                    }
                }
            context.registerReceiver(receiver, IntentFilter("android.media.VOLUME_CHANGED_ACTION"))

            onDispose {
                context.unregisterReceiver(receiver)
            }
        }

        Box(modifier = modifier) {
            IconButton(
                onClick = { showSlider = !showSlider },
            ) {
                val icon = when {
                    volume == 0 -> Icons.AutoMirrored.Filled.VolumeMute
                    volume < maxVolume / 2 -> Icons.AutoMirrored.Filled.VolumeDown
                    else -> Icons.AutoMirrored.Filled.VolumeUp
                }
                Icon(
                    imageVector = icon,
                    contentDescription = stringResource(R.string.volume),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(ICON_SIZE_MEDIUM),
                )
            }

            if (showSlider) {
                Popup(
                    alignment = Alignment.TopCenter,
                    onDismissRequest = {
                        showSlider = false
                    },
                    properties = PopupProperties(focusable = true, clippingEnabled = false),
                    offset = IntOffset(0, -220), // Adjust as needed
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.graphicsLayer { clip = false },
                    ) {
                        VolumeSliderContent(
                            value = volume.toFloat(),
                            onValueChange = { newVal ->
                                val newVolume = newVal.roundToInt()
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                                volume = newVolume
                            },
                            maxVolume = maxVolume.toFloat(),
                            accentColor = accentColor,
                            modifier = Modifier.graphicsLayer { clip = false },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VolumeSliderContent(
    value: Float,
    onValueChange: (Float) -> Unit,
    maxVolume: Float,
    accentColor: Color,
    modifier: Modifier = Modifier,
    initialScrubbingProgress: Float = -1f,
) {
    Surface(
        modifier = modifier
            .width(VOLUME_BAR_WIDTH)
            .height(VOLUME_BAR_HEIGHT)
            .graphicsLayer { clip = false }, // Allow indicator to overflow
        shape = RoundedCornerShape(CORNER_RADIUS_FULL),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = ELEVATION_MEDIUM,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { clip = false }, // Allow indicator to overflow
            contentAlignment = Alignment.Center,
        ) {
            IndicatorSlider(
                value = value,
                onValueChange = onValueChange,
                onValueChangeFinished = { /* Managed by component */ },
                valueRange = 0f..maxVolume,
                modifier = Modifier
                    .graphicsLayer {
                        rotationZ = -90f
                    }
                    .layout { measurable, constraints ->
                        // Swap constraints for rotation
                        val placeable = measurable.measure(
                            constraints.copy(
                                minWidth = constraints.minHeight,
                                maxWidth = constraints.maxHeight,
                                minHeight = constraints.minWidth,
                                maxHeight = constraints.maxWidth,
                            ),
                        )
                        layout(placeable.height, placeable.width) {
                            placeable.place(
                                x = -(placeable.width / 2 - placeable.height / 2),
                                y = -(placeable.height / 2 - placeable.width / 2),
                            )
                        }
                    }
                    .fillMaxSize()
                    .padding(horizontal = ICON_SIZE_MEDIUM),
                colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = accentColor.copy(alpha = 0.3f),
                ),
                indicatorFormatter = { valPos ->
                    val percentage = ((valPos / maxVolume) * 100).roundToInt()
                    "$percentage%"
                },
                indicatorColor = accentColor,
                indicatorAlignment = Alignment.BottomCenter,
                indicatorOffsetY = ICON_SIZE_MEDIUM,
                indicatorRotation = 90f,
                tailDirection = TailDirection.Left,
                initialScrubbingProgress = initialScrubbingProgress,
            )
        }
    }
}

@Composable
@Preview
@Preview(uiMode = PREVIEW_DARK_MODE, name = "VolumeControl_Dark")
fun VolumeControlPreview() {
    VolumeControl()
}

@Composable
@Preview
@Preview(uiMode = PREVIEW_DARK_MODE, name = "VolumeControlWithIndicator_Dark")
fun VolumeControlWithIndicatorPreview() {
    AppTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            // Use a wider Box to contain the Surface and allow the indicator to overflow in previews
            Box(
                modifier = Modifier
                    .width(VOLUME_PREVIEW_WIDTH) // Extra width to prevent clipping of the indicator in preview
                    .height(VOLUME_BAR_HEIGHT),
                contentAlignment = Alignment.Center,
            ) {
                // Use Box with background instead of Surface to avoid clipping in preview
                Box(
                    modifier = Modifier
                        .width(VOLUME_BAR_WIDTH)
                        .height(VOLUME_BAR_HEIGHT)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(CORNER_RADIUS_FULL),
                        ),
                ) {
                    VolumeSliderContent(
                        value = 5f,
                        onValueChange = { },
                        maxVolume = 15f,
                        accentColor = MaterialTheme.colorScheme.primary,
                        initialScrubbingProgress = 7f,
                    )
                }
            }
        }
    }
}
