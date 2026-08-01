package de.carsten.android.muzzic.ui.screens.cards

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import de.carsten.android.muzzic.AppConfig
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.persistence.entity.aggregation.SongPlayCount
import de.carsten.android.muzzic.ui.CARD_INTERNAL_PADDING
import de.carsten.android.muzzic.ui.FONT_SIZE_BODY
import de.carsten.android.muzzic.ui.FONT_SIZE_CAPTION
import de.carsten.android.muzzic.ui.FONT_SIZE_SUBTITLE
import de.carsten.android.muzzic.ui.FONT_SIZE_TITLE
import de.carsten.android.muzzic.ui.RANK_COLUMN_WIDTH
import de.carsten.android.muzzic.ui.RATING_STAR_SIZE
import de.carsten.android.muzzic.ui.SPACING_LARGE
import de.carsten.android.muzzic.ui.theme.AppTheme
import de.carsten.android.muzzic.ui.theme.CustomColors

@Composable
fun SongStatsCard(topSongs: List<SongPlayCount>, titelText: String = stringResource(R.string.songs_distribution).replace("@@{songs}@@", AppConfig.Ui.NUM_OF_TOP_SONGS.toString())) {
    AppTheme {
        MuzzicCard(
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            Text(
                text = titelText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = FONT_SIZE_SUBTITLE,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = SPACING_LARGE),
            )

            topSongs.forEachIndexed { index, songPlayCount ->
                val title = songPlayCount.song.title
                val artist = songPlayCount.song.artist
                val playCount = songPlayCount.totalCount
                val rating = songPlayCount.song.rating

                Row(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = CARD_INTERNAL_PADDING),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "#${index + 1}",
                        color = CustomColors.chartBar,
                        fontSize = FONT_SIZE_TITLE,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(RANK_COLUMN_WIDTH),
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = FONT_SIZE_BODY,
                            fontWeight = FontWeight.SemiBold,
                        )

                        Text(
                            text = artist,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = FONT_SIZE_CAPTION,
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                    ) {
                        Text(
                            text = "$playCount Plays",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = FONT_SIZE_CAPTION,
                            fontWeight = FontWeight.SemiBold,
                        )

                        StarRating(
                            rating = rating,
                            onRatingChanged = { },
                            size = RATING_STAR_SIZE,
                        )
                    }
                }
            }
        }
    }
}
