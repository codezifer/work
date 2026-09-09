package de.carsten.android.muzzic.viewmodel

import app.cash.turbine.test
import de.carsten.android.muzzic.persistence.entity.aggregation.MonthlyPlayCount
import de.carsten.android.muzzic.persistence.repo.MusicRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private lateinit var viewModel: StatisticsViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Mock all stats calls used in init/loadStats
        coEvery { repository.getMonthlyStats() } returns emptyList()
        coEvery { repository.getGenreStats() } returns emptyList()
        coEvery { repository.getTopSongs() } returns emptyList()
        coEvery { repository.getMonthStats() } returns emptyList()
        coEvery { repository.getMonthGenreStats() } returns emptyList()
        coEvery { repository.getTopMonthSongs() } returns emptyList()

        viewModel = StatisticsViewModel(repository)
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
            // Initial state (might be already loaded if init ran fast)
            awaitItem()

            viewModel.loadStats()

            val state = awaitItem()
            assertThat(state.monthlyStats).isEqualTo(monthlyStats)
        }
    }
}
