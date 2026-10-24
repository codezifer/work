package de.carsten.android.muzzic.ui.screens.cards

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
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.persistence.entity.aggregation.ArtistPlayCount
import de.carsten.android.muzzic.ui.CHART_LABEL_COLUMN_WIDTH
import de.carsten.android.muzzic.ui.CHART_ROW_HEIGHT
import de.carsten.android.muzzic.ui.FONT_SIZE_SUBTITLE
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.SPACING_LARGE
import de.carsten.android.muzzic.ui.component.charts.BarChartOrientation
import de.carsten.android.muzzic.ui.component.charts.MuzzicBarChart
import de.carsten.android.muzzic.ui.theme.AppTheme
import de.carsten.android.muzzic.ui.theme.CustomColors

@Composable
fun ArtistPlayCountCard(artistsPlayCounts: List<ArtistPlayCount>) {
    MuzzicCard(
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(
            text = stringResource(R.string.artist_plays),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = FONT_SIZE_SUBTITLE,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = SPACING_LARGE),
        )

        val sorted = artistsPlayCounts.sortedByDescending { it.playCount }

        MuzzicBarChart(
            data = sorted.map { it.playCount.toFloat() },
            colors = listOf(CustomColors.chartBar),
            labels = sorted.map { it.artist },
            // Horizontal bars keep the artist name unrotated in a fixed column on the left; the
            // chart grows by one row per artist instead of squeezing labels into a fixed height.
            orientation = BarChartOrientation.Horizontal,
            modifier = Modifier
                .fillMaxWidth()
                .height(CHART_ROW_HEIGHT * sorted.size),
            barSpacing = SPACING_LARGE,
            labelAreaSize = CHART_LABEL_COLUMN_WIDTH,
            showValuesInside = true,
        )
    }
}

@Composable
@Preview
@Preview(uiMode = PREVIEW_DARK_MODE)
fun ArtistPlayCountAggregationPreview() {
    AppTheme {
        ArtistPlayCountCard(
            artistsPlayCounts = listOf(
                ArtistPlayCount("Dimmu Borgir", "2026-10", 10),
                ArtistPlayCount("Cradle Of Filth", "2026-10", 7),
                ArtistPlayCount("Interpol", "2026-10", 5),
                ArtistPlayCount("Chvrches", "2026-10", 4),
                ArtistPlayCount("Wolfclub", "2026-10", 9),
            ),
        )
    }
}
