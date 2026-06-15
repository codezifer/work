package de.carsten.android.muzzic.viewmodel

import android.app.Application
import androidx.media3.session.MediaBrowser
import de.carsten.android.muzzic.persistence.repo.MusicRepository
import de.carsten.android.muzzic.service.MediaLibraryManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {
    private val repository: MusicRepository = mockk()
    private val mediaLibraryManager: MediaLibraryManager = mockk()
    private val application: Application = mockk()
    private val browser: MediaBrowser = mockk(relaxed = true)

    private val browserFlow = MutableStateFlow<MediaBrowser?>(null)
    private lateinit var viewModel: TestPlayerViewModel
    private val testDispatcher = StandardTestDispatcher()

    class TestPlayerViewModel(
        repository: MusicRepository,
        mediaLibraryManager: MediaLibraryManager,
        application: Application
    ) : PlayerViewModel(repository, mediaLibraryManager, application) {
        // Override methods called in init to avoid side effects
        override fun scanLibrary() {}
        override fun startProgressUpdater() {}
        override fun setupBrowserObservation() {}
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Ensure browser is available immediately in the flow
        browserFlow.value = browser
        every { mediaLibraryManager.browser } returns browserFlow

        // Mocking getAllSongs (called during property initialization)
        every { repository.getAllSongs() } returns flowOf(emptyList())

        // We can't easily avoid the collect in init, but with relaxed mock it should be fine
        // if we set the browser flow before creating the ViewModel
        viewModel = TestPlayerViewModel(repository, mediaLibraryManager, application)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onNextClicked seeks to next if available`() {
        every { browser.hasNextMediaItem() } returns true
        every { browser.seekToNextMediaItem() } returns Unit

        viewModel.onNextClicked()

        verify { browser.seekToNextMediaItem() }
    }

    @Test
    fun `onNextClicked wraps around to start if at end of queue`() {
        every { browser.hasNextMediaItem() } returns false
        every { browser.mediaItemCount } returns 5
        every { browser.seekToDefaultPosition(0) } returns Unit

        viewModel.onNextClicked()

        verify { browser.seekToDefaultPosition(0) }
    }

    @Test
    fun `onPrevClicked seeks to previous if available`() {
        every { browser.hasPreviousMediaItem() } returns true
        every { browser.seekToPreviousMediaItem() } returns Unit

        viewModel.onPrevClicked()

        verify { browser.seekToPreviousMediaItem() }
    }

    @Test
    fun `onPrevClicked wraps around to end if at start of queue`() {
        every { browser.hasPreviousMediaItem() } returns false
        every { browser.mediaItemCount } returns 5
        every { browser.seekToDefaultPosition(4) } returns Unit

        viewModel.onPrevClicked()

        verify { browser.seekToDefaultPosition(4) }
    }
}
