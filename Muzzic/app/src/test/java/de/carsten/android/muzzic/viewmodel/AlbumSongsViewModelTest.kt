package de.carsten.android.muzzic.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.media3.common.util.UnstableApi
import app.cash.turbine.test
import de.carsten.android.muzzic.persistence.repo.AlbumRepository
import de.carsten.android.muzzic.service.MediaLibraryManager
import de.carsten.android.muzzic.ui.model.SongDto
import de.carsten.android.muzzic.ui.state.AlbumSongsUiState
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
class AlbumSongsViewModelTest {
    private val albumRepository: AlbumRepository = mockk()
    private val mediaLibraryManager: MediaLibraryManager = mockk()
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(mapOf("artistName" to "Artist", "albumName" to "Album"))

    private val songsFlow = MutableStateFlow(emptyList<SongDto>())
    private lateinit var viewModel: AlbumSongsViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { albumRepository.getSongsByAlbum("Artist", "Album") } returns songsFlow

        viewModel = AlbumSongsViewModel(savedStateHandle, albumRepository, mediaLibraryManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState reflects songs changes`() = runTest {
        viewModel.uiState.test {
            assertThat(awaitItem()).isEqualTo(AlbumSongsUiState("Artist", "Album", emptyList()))

            val song = SongDto(id = "1", title = "Song")
            songsFlow.value = listOf(song)

            assertThat(awaitItem()).isEqualTo(AlbumSongsUiState("Artist", "Album", listOf(song)))
        }
    }
}
