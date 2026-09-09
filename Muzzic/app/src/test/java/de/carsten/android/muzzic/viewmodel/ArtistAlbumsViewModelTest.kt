package de.carsten.android.muzzic.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.media3.common.util.UnstableApi
import app.cash.turbine.test
import de.carsten.android.muzzic.persistence.repo.AlbumRepository
import de.carsten.android.muzzic.persistence.repo.PlayingQueueRepository
import de.carsten.android.muzzic.service.MediaLibraryManager
import de.carsten.android.muzzic.ui.AppDestinations.ARTIST_ARGUMENT
import de.carsten.android.muzzic.ui.model.AlbumDto
import de.carsten.android.muzzic.ui.state.ArtistAlbumsUiState
import io.mockk.every
import io.mockk.mockk
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

@UnstableApi
@OptIn(ExperimentalCoroutinesApi::class)
class ArtistAlbumsViewModelTest {
    private val albumRepository: AlbumRepository = mockk()
    private val playingQueueRepository: PlayingQueueRepository = mockk()
    private val mediaLibraryManager: MediaLibraryManager = mockk()
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(mapOf(ARTIST_ARGUMENT to "Artist"))

    private val albumsFlow = MutableStateFlow(emptyList<AlbumDto>())
    private lateinit var viewModel: ArtistAlbumsViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { albumRepository.getAlbumsByArtist("Artist") } returns albumsFlow

        viewModel = ArtistAlbumsViewModel(savedStateHandle, albumRepository, playingQueueRepository, mediaLibraryManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState reflects albums changes`() = runTest {
        viewModel.uiState.test {
            assertThat(awaitItem()).isEqualTo(ArtistAlbumsUiState("Artist", emptyList()))

            val album = AlbumDto("Album", 2024, "Artist", 10, 1000)
            albumsFlow.value = listOf(album)

            assertThat(awaitItem()).isEqualTo(ArtistAlbumsUiState("Artist", listOf(album)))
        }
    }
}
