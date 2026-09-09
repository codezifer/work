package de.carsten.android.muzzic.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.media3.common.util.UnstableApi
import app.cash.turbine.test
import de.carsten.android.muzzic.persistence.repo.ArtistRepository
import de.carsten.android.muzzic.persistence.repo.GenreRepository
import de.carsten.android.muzzic.persistence.repo.PlayingQueueRepository
import de.carsten.android.muzzic.service.MediaLibraryManager
import de.carsten.android.muzzic.ui.model.GenreDto
import de.carsten.android.muzzic.ui.state.GenresUiState
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
class GenresViewModelTest {
    private val genreRepository: GenreRepository = mockk()
    private val artistRepository: ArtistRepository = mockk()
    private val playingQueueRepository: PlayingQueueRepository = mockk()
    private val mediaLibraryManager: MediaLibraryManager = mockk()
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()

    private val genresFlow = MutableStateFlow(emptyList<GenreDto>())
    private lateinit var viewModel: GenresViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { genreRepository.getGenreInformation() } returns genresFlow

        viewModel = GenresViewModel(
            savedStateHandle,
            genreRepository,
            artistRepository,
            playingQueueRepository,
            mediaLibraryManager,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial uiState is default and reflects genre changes`() = runTest {
        viewModel.uiState.test {
            assertThat(awaitItem()).isEqualTo(GenresUiState())

            val genre = GenreDto("Rock", 1, 1, 1, 100)
            genresFlow.value = listOf(genre)

            assertThat(awaitItem()).isEqualTo(GenresUiState(genres = listOf(genre)))
        }
    }
}
