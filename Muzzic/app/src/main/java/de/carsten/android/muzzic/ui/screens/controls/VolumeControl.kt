package de.carsten.android.muzzic.ui.screens.controls

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.theme.AppTheme
import kotlin.math.roundToInt

@Composable
fun VolumeControl(modifier: Modifier = Modifier) {
    AppTheme {
        val context = LocalContext.current
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Aktuellen und maximalen Lautstärkewert initial auslesen
        val initialVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

        // Den Zustand des Sliders verwalten
        var volume by remember { mutableIntStateOf(initialVolume) }

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

        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.VolumeDown,
                contentDescription = stringResource(R.string.volume),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )

            Slider(
                value = volume.toFloat(),
                onValueChange = { newVolume ->
                    volume = newVolume.roundToInt()
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
                },
                valueRange = 0f..maxVolume.toFloat(), // Wertebereich an System anpassen
                modifier =
                Modifier
                    .width(150.dp) // Etwas mehr Platz für eine feinere Steuerung
                    .padding(horizontal = 8.dp),
                colors =
                SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                ),
            )

            Icon(
                Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = stringResource(R.string.volume),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
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
