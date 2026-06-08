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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                MuzzicBarChart(
                    data = monthlyPlayCounts.map { it.count.toFloat() },
                    colors = listOf(CustomColors.chartBar),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    barSpacing = 16.dp,
                    showValuesInside = true,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    monthlyPlayCounts.sorted().forEach { month ->
                        Text(
                            text = month.month,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 10.sp,
                            modifier = Modifier.width(24.dp),
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
