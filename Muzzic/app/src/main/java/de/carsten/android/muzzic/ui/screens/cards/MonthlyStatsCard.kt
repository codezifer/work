package de.carsten.android.muzzic.ui.screens.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.persistence.entity.aggregation.MonthlyPlayCount
import de.carsten.android.muzzic.ui.CARD_CONTENT_HEIGHT
import de.carsten.android.muzzic.ui.FONT_SIZE_SMALL
import de.carsten.android.muzzic.ui.FONT_SIZE_SUBTITLE
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.SPACING_LARGE
import de.carsten.android.muzzic.ui.SPACING_MEDIUM
import de.carsten.android.muzzic.ui.component.charts.MuzzicBarChart
import de.carsten.android.muzzic.ui.theme.AppTheme
import de.carsten.android.muzzic.ui.theme.CustomColors

@Composable
fun MonthlyStatsCard(monthlyPlayCounts: List<MonthlyPlayCount>) {
    AppTheme {
        MuzzicCard(
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            Text(
                text = stringResource(R.string.monthly_plays),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = FONT_SIZE_SUBTITLE,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = SPACING_LARGE),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = SPACING_LARGE),
            ) {
                MuzzicBarChart(
                    data = monthlyPlayCounts.map { it.count.toFloat() },
                    colors = listOf(CustomColors.chartBar),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(CARD_CONTENT_HEIGHT),
                    barSpacing = SPACING_LARGE,
                    showValuesInside = true,
                )

                Spacer(modifier = Modifier.height(SPACING_LARGE))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SPACING_MEDIUM),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    monthlyPlayCounts.sorted().forEach { month ->
                        val displayMonth = month.month.replace('-', '/')

                        Text(
                            text = displayMonth,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = FONT_SIZE_SMALL,
                            modifier = Modifier.graphicsLayer {
                                rotationZ = -45f
                            },
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
@Preview()
@Preview(uiMode = PREVIEW_DARK_MODE)
fun MonthlyStatsCardPreview() {
    MonthlyStatsCard(
        listOf(
            MonthlyPlayCount("2026-01", 420),
            MonthlyPlayCount("2026-02", 380),
            MonthlyPlayCount("2026-03", 510),
            MonthlyPlayCount("2026-04", 290),
            MonthlyPlayCount("2026-05", 450),
            MonthlyPlayCount("2026-06", 380),
        ),
    )
}
