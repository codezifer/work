package de.carsten.android.muzzic.ui.screens.cards

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.ui.FONT_SIZE_SUBTITLE
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.SPACING_LARGE
import de.carsten.android.muzzic.ui.SPACING_MEDIUM
import de.carsten.android.muzzic.ui.component.charts.BarChartOrientation
import de.carsten.android.muzzic.ui.component.charts.MuzzicBarChart
import de.carsten.android.muzzic.ui.model.PlaylistDto
import de.carsten.android.muzzic.ui.theme.AppTheme
import java.util.concurrent.TimeUnit

@Composable
fun PlaylistDurationCard(playlistDurations: List<PlaylistDto>) {
    MuzzicCard(
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.playlist_playtime),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = FONT_SIZE_SUBTITLE,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = SPACING_LARGE),
            )

            if (playlistDurations.isNotEmpty()) {
                val data = playlistDurations.map { TimeUnit.MILLISECONDS.toMinutes(it.playlistDuration).toFloat() }
                val labels = playlistDurations.map { it.playlistName }

                MuzzicBarChart(
                    data = data,
                    colors = listOf(MaterialTheme.colorScheme.primary),
                    labels = labels,
                    orientation = BarChartOrientation.Horizontal,
                    showAxis = true,
                    showValuesInside = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((playlistDurations.size * 40).dp) // Dynamic height based on number of playlists
                        .padding(bottom = SPACING_MEDIUM),
                    labelSpacing = 80.dp, // Extra space for playlist names
                )
            } else {
                Text(
                    text = stringResource(R.string.empty_pq), // Reusing empty string or add new one
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = SPACING_MEDIUM),
                )
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
@Preview(uiMode = PREVIEW_DARK_MODE, showBackground = true)
fun PlaylistDurationCardPreview() {
    AppTheme {
        PlaylistDurationCard(
            playlistDurations = listOf(
                PlaylistDto("1", "Workout", false, null, 10, 2, 20, 3600000),
                PlaylistDto("2", "Relax", false, null, 15, 3, 30, 7200000),
                PlaylistDto("3", "Party", false, null, 20, 4, 40, 10800000),
            ),
        )
    }
}
