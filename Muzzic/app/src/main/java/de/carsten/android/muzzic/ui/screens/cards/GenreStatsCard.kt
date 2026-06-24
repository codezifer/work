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
import androidx.compose.ui.unit.dp
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.persistence.entity.aggregation.GenrePlayCount
import de.carsten.android.muzzic.ui.FONT_SIZE_CAPTION
import de.carsten.android.muzzic.ui.FONT_SIZE_SUBTITLE
import de.carsten.android.muzzic.ui.ICON_SIZE_TINY
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.SPACING_EXTRA_LARGE
import de.carsten.android.muzzic.ui.SPACING_LARGE
import de.carsten.android.muzzic.ui.SPACING_MEDIUM
import de.carsten.android.muzzic.ui.SPACING_SMALL
import de.carsten.android.muzzic.ui.component.charts.MuzzicPieChart
import de.carsten.android.muzzic.ui.theme.AppTheme
import de.carsten.android.muzzic.ui.theme.CustomColors

@Composable
fun GenreStatsCard(genreStats: List<GenrePlayCount>) {
    AppTheme {
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
                text = stringResource(R.string.genre_distribution),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = FONT_SIZE_SUBTITLE,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = SPACING_LARGE),
            )

            val allGenresCount = genreStats.sumOf { it.count }
            val genresWithColor = genreStats.sorted().mapIndexed { index, genrePlayCount ->
                val colorIndex = index % colors.size
                val genreColor = colors[colorIndex]
                Pair(genrePlayCount, genreColor)
            }

            if (allGenresCount > 0) {
                val data = genresWithColor.map { it.first.count.toFloat() }
                val labels = data.map { value ->
                    val percentage = (value / allGenresCount * 100).toInt()
                    if (percentage > 5) "$percentage%" else ""
                }

                MuzzicPieChart(
                    data = data,
                    colors = genresWithColor.map { it.second },
                    labels = labels,
                    holeRadiusPercent = 0.4f,
                    modifier = Modifier
                        .size(160.dp) // I'll keep 160.dp for now or use a constant. I'll use 160.dp.
                        .padding(bottom = SPACING_EXTRA_LARGE),
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                genresWithColor.forEach { (genrePlayCount, color) ->
                    val percentage = if (allGenresCount > 0) {
                        (genrePlayCount.count.toFloat() / allGenresCount * 100).toInt()
                    } else {
                        0
                    }

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
                            text = genrePlayCount.genre,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = FONT_SIZE_CAPTION,
                            modifier = Modifier.weight(1f),
                        )

                        Text(
                            text = "$percentage%",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = FONT_SIZE_CAPTION,
                        )
                    }
                }
            }
        }
    }
}

@Composable
@Preview
@Preview(uiMode = PREVIEW_DARK_MODE)
fun GenreStatsCardPreview() {
    GenreStatsCard(
        genreStats = listOf(
            GenrePlayCount("Rock", 100),
            GenrePlayCount("Pop", 80),
            GenrePlayCount("Jazz", 60),
            GenrePlayCount("Classical", 40),
            GenrePlayCount("Electronic", 20),
        ),
    )
}
