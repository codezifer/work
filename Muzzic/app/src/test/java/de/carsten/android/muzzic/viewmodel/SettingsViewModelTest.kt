package de.carsten.android.muzzic.viewmodel

import androidx.test.ext.junit.runners.AndroidJUnit4
import de.carsten.android.muzzic.persistence.repo.AppSettingsRepository
import de.carsten.android.muzzic.persistence.repo.MusicRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SettingsViewModelTest {
    private val appSettingsRepository: AppSettingsRepository = mockk()
    private val musicRepository: MusicRepository = mockk()

    private lateinit var viewModel: SettingsViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        every { appSettingsRepository.observeMusicDirectory() } returns flowOf(null)
        every { appSettingsRepository.observePlaylistDirectory() } returns flowOf(null)
        Dispatchers.setMain(testDispatcher)
        viewModel = SettingsViewModel(appSettingsRepository, musicRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `clearLibrary clears the library via repository`() = runTest {
        coEvery { musicRepository.clearLibrary() } returns Unit

        viewModel.clearLibrary()
        testScheduler.advanceUntilIdle()

        coVerify { musicRepository.clearLibrary() }
    }

    @Test
    fun `scanMusicLibrary triggers library scan`() = runTest {
        every { musicRepository.scanMusicLibrary() } returns Unit

        viewModel.scanMusicLibrary()
        testScheduler.advanceUntilIdle()

        verify { musicRepository.scanMusicLibrary() }
    }

    @Test
    fun `importPlaylists triggers playlist import`() = runTest {
        every { musicRepository.importPlaylists() } returns Unit

        viewModel.importPlaylists()
        testScheduler.advanceUntilIdle()

        verify { musicRepository.importPlaylists() }
    }
}
