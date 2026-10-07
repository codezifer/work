package de.carsten.android.muzzic.viewmodel

import app.cash.turbine.test
import de.carsten.android.muzzic.persistence.entity.aggregation.MonthlyPlayCount
import de.carsten.android.muzzic.persistence.repo.AlbumRepository
import de.carsten.android.muzzic.persistence.repo.ArtistRepository
import de.carsten.android.muzzic.persistence.repo.GenreRepository
import de.carsten.android.muzzic.persistence.repo.MusicRepository
import de.carsten.android.muzzic.persistence.repo.PlaylistRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModelTest {
    private val repository: MusicRepository = mockk()
    private val genreRepository: GenreRepository = mockk()
    private val playlistRepository: PlaylistRepository = mockk()
    private val artistRepository: ArtistRepository = mockk()
    private val albumRepository: AlbumRepository = mockk()
    private lateinit var viewModel: StatisticsViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Mock all stats calls used in loadStats
        coEvery { genreRepository.getGenreCounts() } returns emptyList()
        coEvery { repository.getMonthlyStats() } returns emptyList()
        coEvery { repository.getGenreStats(any()) } returns emptyList()
        coEvery { repository.getTopSongs() } returns emptyList()
        coEvery { repository.getMonthStats() } returns emptyList()
        coEvery { repository.getMonthGenreStats(any()) } returns emptyList()
        coEvery { repository.getTopMonthSongs() } returns emptyList()
        coEvery { repository.getLibrarySummary() } returns mockk(relaxed = true)
        every { genreRepository.getGenreInformation() } returns flowOf(emptyList())
        every { playlistRepository.getPlaylistInformation() } returns flowOf(emptyList())
        coEvery { artistRepository.getRecentArtists() } returns emptyList()
        coEvery { albumRepository.getRecentAlbums() } returns emptyList()

        viewModel = StatisticsViewModel(repository, genreRepository, playlistRepository, artistRepository, albumRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadStats updates uiState with repository data`() = runTest {
        val monthlyStats = listOf(MonthlyPlayCount("2024-01", 10))
        coEvery { repository.getMonthlyStats() } returns monthlyStats

        viewModel.uiState.test {
            // Initial state, no auto-load in init anymore
            assertThat(awaitItem().monthlyStats).isEmpty()

            viewModel.loadStats()

            val state = awaitItem()
            assertThat(state.monthlyStats).isEqualTo(monthlyStats)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadTab loads only the requested tab group`() = runTest {
        val monthSongs = listOf(
            de.carsten.android.muzzic.persistence.entity.aggregation.SongPlayCount(
                song = de.carsten.android.muzzic.persistence.entity.Song(title = "Tab Test"),
                totalCount = 5,
            ),
        )
        coEvery { repository.getTopMonthSongs() } returns monthSongs

        viewModel.uiState.test {
            assertThat(awaitItem().topMonthSongs).isEmpty()

            viewModel.loadTab(StatisticsViewModel.TAB_MONTH)

            val state = awaitItem()
            assertThat(state.topMonthSongs).isEqualTo(monthSongs)
            assertThat(state.monthlyStats).isEmpty()
            assertThat(state.librarySummary).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadTab library loads recent artists and albums`() = runTest {
        val artists = listOf(de.carsten.android.muzzic.ui.model.ArtistDto("New Artist", 1, 5))
        val albums = listOf(
            de.carsten.android.muzzic.ui.model.AlbumDto(
                artistName = "New Artist",
                albumName = "New Album",
                albumYear = 2026,
                songCount = 5,
                albumDuration = 1000L,
            ),
        )
        coEvery { artistRepository.getRecentArtists() } returns artists
        coEvery { albumRepository.getRecentAlbums() } returns albums

        viewModel.uiState.test {
            assertThat(awaitItem().recentArtists).isEmpty()

            viewModel.loadTab(StatisticsViewModel.TAB_LIBRARY)

            val state = awaitItem()
            assertThat(state.recentArtists).isEqualTo(artists)
            assertThat(state.recentAlbums).isEqualTo(albums)
            assertThat(state.monthlyStats).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
