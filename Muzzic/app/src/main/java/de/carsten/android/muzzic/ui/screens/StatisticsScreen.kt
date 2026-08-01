package de.carsten.android.muzzic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.persistence.entity.Song
import de.carsten.android.muzzic.persistence.entity.aggregation.GenrePlayCount
import de.carsten.android.muzzic.persistence.entity.aggregation.MonthlyPlayCount
import de.carsten.android.muzzic.persistence.entity.aggregation.SongPlayCount
import de.carsten.android.muzzic.ui.FONT_SIZE_HUGE_TITLE
import de.carsten.android.muzzic.ui.GLASS_CONTAINER_ALPHA
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.SPACING_LARGE
import de.carsten.android.muzzic.ui.navigation.MusicAppState
import de.carsten.android.muzzic.ui.screens.cards.GenreStatsCard
import de.carsten.android.muzzic.ui.screens.cards.MonthlyStatsCard
import de.carsten.android.muzzic.ui.screens.cards.SongStatsCard
import de.carsten.android.muzzic.ui.theme.AppTheme
import de.carsten.android.muzzic.viewmodel.StatisticsViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun StatisticsScreen(modifier: Modifier = Modifier, appState: MusicAppState, viewModel: StatisticsViewModel = koinViewModel()) {
    // Refresh statistics from the database every time the user navigates to this screen
    LaunchedEffect(Unit) {
        viewModel.loadStats()
    }

    val months by viewModel.monthlyStats.collectAsStateWithLifecycle()
    val songs by viewModel.topSongs.collectAsStateWithLifecycle()
    val genres by viewModel.genreStats.collectAsStateWithLifecycle()
    val monthStats by viewModel.monthStats.collectAsStateWithLifecycle()
    val monthGenres by viewModel.monthGenreStats.collectAsStateWithLifecycle()
    val monthSongs by viewModel.topMonthSongs.collectAsStateWithLifecycle()

    StatisticsScreenContent(
        modifier = modifier,
        appState = appState,
        months = months,
        genres = genres,
        songs = songs,
        monthStats = monthStats,
        monthGenres = monthGenres,
        monthSongs = monthSongs,
    )
}

@Composable
private fun StatisticsScreenContent(
    modifier: Modifier = Modifier,
    appState: MusicAppState? = null,
    months: List<MonthlyPlayCount>,
    genres: List<GenrePlayCount>,
    songs: List<SongPlayCount>,
    monthStats: List<MonthlyPlayCount> = emptyList(),
    monthGenres: List<GenrePlayCount> = emptyList(),
    monthSongs: List<SongPlayCount> = emptyList(),
    initialTabIndex: Int = 0,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTabIndex) }
    val tabs = listOf(
        stringResource(R.string.stats_tab_overall),
        stringResource(R.string.stats_tab_month),
    )

    Column(
        modifier =
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = GLASS_CONTAINER_ALPHA)),
    ) {
        Text(
            text = stringResource(R.string.statistics),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = FONT_SIZE_HUGE_TITLE,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = SPACING_LARGE, vertical = SPACING_LARGE),
        )

        PrimaryTabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(text = title) },
                )
            }
        }

        LazyColumn(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = SPACING_LARGE, vertical = SPACING_LARGE),
            verticalArrangement = Arrangement.spacedBy(SPACING_LARGE),
        ) {
            if (selectedTab == 0) {
                // Overall statistics (based on cumulative song data)
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
            } else {
                // Current month statistics (based on play_history)
                item {
                    MonthlyStatsCard(monthStats)
                }

                // Genre distribution of the current month
                item {
                    GenreStatsCard(monthGenres)
                }

                // Top songs of the current month
                item {
                    SongStatsCard(topSongs = monthSongs, titelText = stringResource(R.string.top_songs_month))
                }
            }
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
                MonthlyPlayCount("2026-01", 21),
                MonthlyPlayCount("2026-02", 42),
                MonthlyPlayCount("2026-03", 34),
                MonthlyPlayCount("2026-04", 56),
                MonthlyPlayCount("2026-05", 61),
                MonthlyPlayCount("2026-06", 73),
            ),
        )
    }
}

@Composable
@Preview(showBackground = true)
@Preview(uiMode = PREVIEW_DARK_MODE, showBackground = true)
fun StatisticsScreenMonthPreview() {
    AppTheme {
        StatisticsScreenContent(
            months = emptyList(),
            genres = emptyList(),
            songs = emptyList(),
            monthStats = listOf(
                MonthlyPlayCount("2026-02", 39),
                MonthlyPlayCount("2026-03", 71),
                MonthlyPlayCount("2026-04", 48),
                MonthlyPlayCount("2026-05", 55),
                MonthlyPlayCount("2026-06", 82),
                MonthlyPlayCount("2026-07", 66),
                MonthlyPlayCount("2026-08", 92),
            ),
            monthGenres = listOf(
                GenrePlayCount("Black Metal", 45),
                GenrePlayCount("Post Punk", 30),
                GenrePlayCount("Ambient", 17),
            ),
            monthSongs = listOf(
                SongPlayCount(
                    song = Song(
                        title = "Echoes of the Void",
                        playCount = 3,
                        artist = "Test 1",
                    ),
                    totalCount = 12,
                ),
                SongPlayCount(
                    song = Song(
                        title = "Silent Horizon",
                        playCount = 5,
                        artist = "Test 2",
                    ),
                    totalCount = 9,
                ),
                SongPlayCount(
                    song = Song(
                        title = "Midnight Pulse",
                        playCount = 2,
                        artist = "Test 3",
                    ),
                    totalCount = 6,
                ),
            ),
            initialTabIndex = 1,
        )
    }
}
