package de.carsten.android.muzzic.viewmodel

import android.app.Application
import androidx.media3.session.MediaBrowser
import app.cash.turbine.test
import de.carsten.android.muzzic.persistence.repo.AppSettingsRepository
import de.carsten.android.muzzic.persistence.repo.MusicRepository
import de.carsten.android.muzzic.service.MediaLibraryManager
import de.carsten.android.muzzic.ui.state.PlayerUiState
import de.carsten.android.muzzic.visualization.component.VisualizerEngine
import de.carsten.android.muzzic.visualization.service.VisualizerSink
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
class PlayerViewModelTest {
    private val repository: MusicRepository = mockk()
    private val mediaLibraryManager: MediaLibraryManager = mockk()
    private val visualizerSink: VisualizerSink = mockk()
    private val appSettingsRepository: AppSettingsRepository = mockk()
    private val application: Application = mockk()
    private val browser: MediaBrowser = mockk(relaxed = true)

    private val browserFlow = MutableStateFlow<MediaBrowser?>(null)
    private val amplitudesFlow = MutableStateFlow<List<Float>>(emptyList())
    private val engineFlow = MutableStateFlow(VisualizerEngine.BARS)
    private val presetFlow = MutableStateFlow<String?>(null)

    private lateinit var viewModel: TestPlayerViewModel
    private val testDispatcher = StandardTestDispatcher()

    class TestPlayerViewModel(
        repository: MusicRepository,
        mediaLibraryManager: MediaLibraryManager,
        visualizerSink: VisualizerSink,
        appSettingsRepository: AppSettingsRepository,
        application: Application,
    ) : PlayerViewModel(repository, mediaLibraryManager, visualizerSink, appSettingsRepository, application) {
        override fun scanLibrary() {}
        override fun startProgressUpdater() {}
        var shouldObserveBrowser = false
        override fun setupBrowserObservation() {
            if (shouldObserveBrowser) super.setupBrowserObservation()
        }
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        browserFlow.value = browser
        every { mediaLibraryManager.browser } returns browserFlow
        every { visualizerSink.amplitudes } returns amplitudesFlow
        every { repository.getAllSongs() } returns flowOf(emptyList())
        every { appSettingsRepository.observeVisualizerEngine() } returns engineFlow
        every { appSettingsRepository.observeProjectMPreset() } returns presetFlow
        viewModel = TestPlayerViewModel(repository, mediaLibraryManager, visualizerSink, appSettingsRepository, application)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial uiState is default`() = runTest {
        viewModel.uiState.test {
            assertThat(awaitItem()).isEqualTo(PlayerUiState())
        }
    }

    @Test
    fun `amplitudes updates propagate to uiState`() = runTest {
        viewModel.uiState.test {
            assertThat(awaitItem().amplitudes).isEmpty()

            val newAmps = listOf(0.1f, 0.2f)
            amplitudesFlow.value = newAmps

            assertThat(awaitItem().amplitudes).isEqualTo(newAmps)
        }
    }

    @Test
    fun `visualizer engine settings update propagates to uiState`() = runTest {
        viewModel.uiState.test {
            assertThat(awaitItem().visualizerEngine).isEqualTo(VisualizerEngine.BARS)

            engineFlow.value = VisualizerEngine.PROJECT_M
            assertThat(awaitItem().visualizerEngine).isEqualTo(VisualizerEngine.PROJECT_M)
        }
    }

    @Test
    fun `onNextClicked seeks to next if available`() {
        every { browser.hasNextMediaItem() } returns true
        every { browser.seekToNextMediaItem() } returns Unit

        viewModel.onNextClicked()

        verify { browser.seekToNextMediaItem() }
    }

    @Test
    fun `onPrevClicked seeks to previous if available`() {
        every { browser.hasPreviousMediaItem() } returns true
        every { browser.seekToPreviousMediaItem() } returns Unit

        viewModel.onPrevClicked()

        verify { browser.seekToPreviousMediaItem() }
    }
}
