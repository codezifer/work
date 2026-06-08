package de.carsten.android.muzzic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.persistence.entity.Song
import de.carsten.android.muzzic.persistence.entity.aggregation.GenrePlayCount
import de.carsten.android.muzzic.persistence.entity.aggregation.MonthlyPlayCount
import de.carsten.android.muzzic.persistence.entity.aggregation.SongPlayCount
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.navigation.MusicAppState
import de.carsten.android.muzzic.ui.screens.cards.GenreStatsCard
import de.carsten.android.muzzic.ui.screens.cards.MonthlyStatsCard
import de.carsten.android.muzzic.ui.screens.cards.SongStatsCard
import de.carsten.android.muzzic.ui.theme.AppTheme
import de.carsten.android.muzzic.viewmodel.StatisticsViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun StatisticsScreen(modifier: Modifier = Modifier, appState: MusicAppState, viewModel: StatisticsViewModel = koinViewModel()) {
    val months by viewModel.monthlyStats.collectAsStateWithLifecycle()
    val songs by viewModel.topSongs.collectAsStateWithLifecycle()
    val genres by viewModel.genreStats.collectAsStateWithLifecycle()

    StatisticsScreenContent(
        modifier = modifier,
        appState = appState,
        months = months,
        genres = genres,
        songs = songs,
    )
}

@Composable
private fun StatisticsScreenContent(
    modifier: Modifier = Modifier,
    appState: MusicAppState? = null,
    months: List<MonthlyPlayCount>,
    genres: List<GenrePlayCount>,
    songs: List<SongPlayCount>,
) {
    LazyColumn(
        modifier =
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header
        item {
            Text(
                text = stringResource(R.string.statistics),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }

        // Monthly stats. chart
        item {
            MonthlyStatsCard(months)
        }

        // Genre distribution
        item {
            GenreStatsCard(genres)
        }

        // Top songs
        item {
            SongStatsCard(songs)
        }
    }
}

@Composable
@Preview(showBackground = true)
@Preview(uiMode = PREVIEW_DARK_MODE, showBackground = true)
fun StatisticsScreenPreview() {
    AppTheme {
        StatisticsScreenContent(
            genres = listOf(
                GenrePlayCount("Black Metal", 65),
                GenrePlayCount("Post Punk", 35),
            ),
            songs = listOf(
                SongPlayCount(
                    song = Song(
                        title = "Test 1",
                        playCount = 4,
                        artist = "Test 1",
                    ),
                    totalCount = 14,
                ),
                SongPlayCount(
                    song = Song(
                        title = "Test 2",
                        playCount = 7,
                        artist = "Test 2",
                    ),
                    totalCount = 22,
                ),
            ),
            months = listOf(
                MonthlyPlayCount("Jan", 21),
                MonthlyPlayCount("Feb", 42),
                MonthlyPlayCount("Mar", 34),
                MonthlyPlayCount("Apr", 56),
                MonthlyPlayCount("May", 61),
                MonthlyPlayCount("Jun", 73),
            ),
        )
    }
}
