package de.carsten.android.muzzic.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import de.carsten.android.muzzic.persistence.repo.AppSettingsRepository
import de.carsten.android.muzzic.persistence.repo.MusicRepository
import de.carsten.android.muzzic.visualization.component.VisualizerEngine
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SettingsViewModelTest {
    private val appSettingsRepository: AppSettingsRepository = mockk()
    private val musicRepository: MusicRepository = mockk()
    private val application: Application = ApplicationProvider.getApplicationContext()

    private val musicDirFlow = MutableStateFlow<String?>(null)
    private val playlistDirFlow = MutableStateFlow<String?>(null)
    private val engineFlow = MutableStateFlow(VisualizerEngine.BARS)
    private val presetFlow = MutableStateFlow<String?>(null)

    private lateinit var viewModel: SettingsViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        every { appSettingsRepository.observeMusicDirectory() } returns musicDirFlow
        every { appSettingsRepository.observePlaylistDirectory() } returns playlistDirFlow
        every { appSettingsRepository.observeVisualizerEngine() } returns engineFlow
        every { appSettingsRepository.observeProjectMPreset() } returns presetFlow
        Dispatchers.setMain(testDispatcher)
        viewModel = SettingsViewModel(appSettingsRepository, musicRepository, application)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState reflects repository changes`() = runTest {
        viewModel.uiState.test {
            val initial = awaitItem()
            assertThat(initial.musicDirectory).isNull()
            assertThat(initial.playlistDirectory).isNull()
            assertThat(initial.visualizerEngine).isEqualTo(VisualizerEngine.BARS)

            musicDirFlow.value = "/path/to/music"
            val item1 = awaitItem()
            assertThat(item1.musicDirectory).isEqualTo("/path/to/music")

            engineFlow.value = VisualizerEngine.PROJECT_M
            val item2 = awaitItem()
            assertThat(item2.visualizerEngine).isEqualTo(VisualizerEngine.PROJECT_M)
        }
    }

    @Test
    fun `updateVisualizerEngine updates repository`() = runTest {
        coEvery { appSettingsRepository.saveVisualizerEngine(VisualizerEngine.PROJECT_M) } returns Unit

        viewModel.updateVisualizerEngine(VisualizerEngine.PROJECT_M)
        testScheduler.advanceUntilIdle()

        coVerify { appSettingsRepository.saveVisualizerEngine(VisualizerEngine.PROJECT_M) }
    }

    @Test
    fun `updateProjectMPreset updates repository`() = runTest {
        coEvery { appSettingsRepository.saveProjectMPreset("test.milk") } returns Unit

        viewModel.updateProjectMPreset("test.milk")
        testScheduler.advanceUntilIdle()

        coVerify { appSettingsRepository.saveProjectMPreset("test.milk") }
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
