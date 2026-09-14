package de.carsten.android.muzzic.ui.screens.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.persistence.entity.aggregation.LibrarySummary
import de.carsten.android.muzzic.ui.FONT_SIZE_BODY
import de.carsten.android.muzzic.ui.FONT_SIZE_SUBTITLE
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.SPACING_LARGE
import de.carsten.android.muzzic.ui.SPACING_SMALL
import de.carsten.android.muzzic.ui.theme.AppTheme

@Composable
fun LibrarySummaryCard(summary: LibrarySummary) {
    MuzzicCard(
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.library_summary),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = FONT_SIZE_SUBTITLE,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = SPACING_LARGE),
            )

            SummaryItem(label = stringResource(R.string.artists), count = summary.artistCount)
            SummaryItem(label = stringResource(R.string.albums), count = summary.albumCount)
            SummaryItem(label = stringResource(R.string.songs), count = summary.songCount)
            SummaryItem(label = stringResource(R.string.genres), count = summary.genreCount)
            SummaryItem(label = stringResource(R.string.playlists), count = summary.playlistCount)
        }
    }
}

@Composable
private fun SummaryItem(label: String, count: Int) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(vertical = SPACING_SMALL),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = FONT_SIZE_BODY,
        )
        Text(
            text = count.toString(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = FONT_SIZE_BODY,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
@Preview(showBackground = true)
@Preview(uiMode = PREVIEW_DARK_MODE, showBackground = true)
fun LibrarySummaryCardPreview() {
    AppTheme {
        LibrarySummaryCard(
            summary = LibrarySummary(
                artistCount = 120,
                albumCount = 250,
                songCount = 3500,
                genreCount = 15,
                playlistCount = 10,
            ),
        )
    }
}
