package de.carsten.android.muzzic.ui.screens.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import de.carsten.android.muzzic.ui.CARD_CONTENT_HEIGHT
import de.carsten.android.muzzic.ui.FONT_SIZE_SMALL
import de.carsten.android.muzzic.ui.FONT_SIZE_SUBTITLE
import de.carsten.android.muzzic.ui.ICON_SIZE_MEDIUM
import de.carsten.android.muzzic.ui.SPACING_LARGE
import de.carsten.android.muzzic.ui.SPACING_MEDIUM
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.persistence.entity.aggregation.MonthlyPlayCount
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
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

            Column(modifier = Modifier.fillMaxWidth()) {
                MuzzicBarChart(
                    data = monthlyPlayCounts.map { it.count.toFloat() },
                    colors = listOf(CustomColors.chartBar),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(CARD_CONTENT_HEIGHT),
                    barSpacing = SPACING_LARGE,
                    showValuesInside = true,
                )

                Spacer(modifier = Modifier.height(SPACING_MEDIUM))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    monthlyPlayCounts.sorted().forEach { month ->
                        Text(
                            text = month.month,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = FONT_SIZE_SMALL,
                            modifier = Modifier.width(ICON_SIZE_MEDIUM),
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
            MonthlyPlayCount("Jan", 420),
            MonthlyPlayCount("Feb", 380),
            MonthlyPlayCount("Mar", 510),
            MonthlyPlayCount("Apr", 290),
            MonthlyPlayCount("May", 450),
            MonthlyPlayCount("Jun", 380),
        ),
    )
}
