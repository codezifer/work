package de.carsten.android.muzzic.ui.screens.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.ui.CHART_PERCENTAGE_THRESHOLD
import de.carsten.android.muzzic.ui.CHART_PIE_HOLE_RADIUS
import de.carsten.android.muzzic.ui.CHART_PIE_SIZE
import de.carsten.android.muzzic.ui.FONT_SIZE_CAPTION
import de.carsten.android.muzzic.ui.FONT_SIZE_SUBTITLE
import de.carsten.android.muzzic.ui.ICON_SIZE_TINY
import de.carsten.android.muzzic.ui.PERCENTAGE_FACTOR
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.SPACING_EXTRA_LARGE
import de.carsten.android.muzzic.ui.SPACING_LARGE
import de.carsten.android.muzzic.ui.SPACING_MEDIUM
import de.carsten.android.muzzic.ui.SPACING_SMALL
import de.carsten.android.muzzic.ui.component.charts.MuzzicPieChart
import de.carsten.android.muzzic.ui.model.GenreDto
import de.carsten.android.muzzic.ui.theme.AppTheme
import de.carsten.android.muzzic.ui.theme.CustomColors
import java.util.concurrent.TimeUnit

@Composable
fun GenreDurationCard(genreDurations: List<GenreDto>) {
    val colors = listOf(
        CustomColors.genre1,
        CustomColors.genre2,
        CustomColors.genre3,
        CustomColors.genre4,
        CustomColors.genre5,
    )

    MuzzicCard(
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(
            text = stringResource(R.string.genre_playtime),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = FONT_SIZE_SUBTITLE,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = SPACING_LARGE),
        )

        val totalDuration = genreDurations.sumOf { it.genreDuration }
        val genresWithColor = genreDurations
            .sortedByDescending { it.genreDuration }
            .take(10) // Limit to top 10 for readability
            .mapIndexed { index, genreDto ->
                val colorIndex = index % colors.size
                val genreColor = colors[colorIndex]
                Pair(genreDto, genreColor)
            }

        if (totalDuration > 0) {
            val data = genresWithColor.map { it.first.genreDuration.toFloat() }
            val labels = data.map { value ->
                val percentage = ((value / totalDuration) * PERCENTAGE_FACTOR).toInt()
                if (percentage > CHART_PERCENTAGE_THRESHOLD) "$percentage%" else ""
            }

            MuzzicPieChart(
                data = data,
                colors = genresWithColor.map { it.second },
                labels = labels,
                holeRadiusPercent = CHART_PIE_HOLE_RADIUS,
                modifier = Modifier
                    .size(CHART_PIE_SIZE)
                    .padding(bottom = SPACING_EXTRA_LARGE),
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            genresWithColor.forEach { (genreDto, color) ->
                val minutes = TimeUnit.MILLISECONDS.toMinutes(genreDto.genreDuration)

                Row(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = SPACING_SMALL),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier =
                        Modifier
                            .size(ICON_SIZE_TINY)
                            .background(color, CircleShape),
                    )

                    Spacer(modifier = Modifier.width(SPACING_MEDIUM))

                    Text(
                        text = genreDto.genreName,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = FONT_SIZE_CAPTION,
                        modifier = Modifier.weight(1f),
                    )

                    Text(
                        text = stringResource(R.string.duration_minutes, minutes),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = FONT_SIZE_CAPTION,
                    )
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
@Preview(uiMode = PREVIEW_DARK_MODE, showBackground = true)
fun GenreDurationCardPreview() {
    AppTheme {
        GenreDurationCard(
            genreDurations = listOf(
                GenreDto("Rock", 10, 5, 50, 3600000),
                GenreDto("Pop", 8, 4, 40, 2400000),
                GenreDto("Jazz", 6, 3, 30, 1800000),
            ),
        )
    }
}
