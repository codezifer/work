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
import de.carsten.android.muzzic.persistence.entity.aggregation.LibrarySummary
import de.carsten.android.muzzic.persistence.entity.aggregation.MonthlyPlayCount
import de.carsten.android.muzzic.persistence.entity.aggregation.SongPlayCount
import de.carsten.android.muzzic.ui.FONT_SIZE_HUGE_TITLE
import de.carsten.android.muzzic.ui.GLASS_CONTAINER_ALPHA
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.SPACING_LARGE
import de.carsten.android.muzzic.ui.model.GenreDto
import de.carsten.android.muzzic.ui.model.PlaylistDto
import de.carsten.android.muzzic.ui.navigation.MusicAppState
import de.carsten.android.muzzic.ui.screens.cards.GenreDurationCard
import de.carsten.android.muzzic.ui.screens.cards.GenreStatsCard
import de.carsten.android.muzzic.ui.screens.cards.LibrarySummaryCard
import de.carsten.android.muzzic.ui.screens.cards.MonthlyStatsCard
import de.carsten.android.muzzic.ui.screens.cards.PlaylistDurationCard
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

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    StatisticsScreenContent(
        modifier = modifier,
        appState = appState,
        months = uiState.monthlyStats,
        genres = uiState.genreStats,
        songs = uiState.topSongs,
        monthStats = uiState.monthStats,
        monthGenres = uiState.monthGenreStats,
        monthSongs = uiState.topMonthSongs,
        librarySummary = uiState.librarySummary,
        genreDurations = uiState.genreDurations,
        playlistDurations = uiState.playlistDurations,
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
    librarySummary: LibrarySummary? = null,
    genreDurations: List<GenreDto> = emptyList(),
    playlistDurations: List<PlaylistDto> = emptyList(),
    initialTabIndex: Int = 0,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTabIndex) }
    val tabs = listOf(
        stringResource(R.string.stats_tab_overall),
        stringResource(R.string.stats_tab_month),
        stringResource(R.string.stats_tab_library),
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
            when (selectedTab) {
                0 -> {
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
                }

                1 -> {
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

                2 -> {
                    // Library summary
                    librarySummary?.let {
                        item {
                            LibrarySummaryCard(it)
                        }
                    }

                    // Genre playtime distribution
                    if (genreDurations.isNotEmpty()) {
                        item {
                            GenreDurationCard(genreDurations)
                        }
                    }

                    // Playlist playtime distribution
                    if (playlistDurations.isNotEmpty()) {
                        item {
                            PlaylistDurationCard(playlistDurations)
                        }
                    }
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

@Composable
@Preview(showBackground = true)
@Preview(uiMode = PREVIEW_DARK_MODE, showBackground = true)
fun StatisticsScreenLibraryPreview() {
    AppTheme {
        StatisticsScreenContent(
            months = emptyList(),
            genres = emptyList(),
            songs = emptyList(),
            librarySummary = LibrarySummary(
                artistCount = 142,
                albumCount = 328,
                songCount = 4512,
                genreCount = 18,
                playlistCount = 12,
            ),
            genreDurations = listOf(
                GenreDto("Black Metal", 25, 12, 120, 43200000),
                GenreDto("Post Punk", 18, 9, 85, 28800000),
                GenreDto("Ambient", 12, 6, 60, 21600000),
                GenreDto("Doom Metal", 10, 5, 45, 18000000),
            ),
            playlistDurations = listOf(
                PlaylistDto("1", "Night Drive", false, null, 20, 5, 40, 7200000),
                PlaylistDto("2", "Study Focus", false, null, 15, 4, 35, 5400000),
                PlaylistDto("3", "Morning Ritual", false, null, 10, 3, 25, 3600000),
            ),
            initialTabIndex = 2,
        )
    }
}
