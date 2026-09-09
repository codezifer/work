package de.carsten.android.muzzic.viewmodel

import androidx.media3.common.util.UnstableApi
import app.cash.turbine.test
import de.carsten.android.muzzic.persistence.repo.AlbumRepository
import de.carsten.android.muzzic.persistence.repo.ArtistRepository
import de.carsten.android.muzzic.persistence.repo.GenreRepository
import de.carsten.android.muzzic.persistence.repo.MusicRepository
import de.carsten.android.muzzic.persistence.repo.PlayingQueueRepository
import de.carsten.android.muzzic.persistence.repo.PlaylistRepository
import de.carsten.android.muzzic.service.MediaLibraryManager
import de.carsten.android.muzzic.ui.model.AlbumDto
import de.carsten.android.muzzic.ui.model.ArtistDto
import de.carsten.android.muzzic.ui.model.GenreDto
import de.carsten.android.muzzic.ui.model.PlaylistDto
import de.carsten.android.muzzic.ui.model.SongDto
import de.carsten.android.muzzic.ui.state.LibraryUiState
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
class LibraryViewModelTest {
    private val musicRepository: MusicRepository = mockk()
    private val artistRepository: ArtistRepository = mockk()
    private val albumRepository: AlbumRepository = mockk()
    private val genreRepository: GenreRepository = mockk()
    private val playlistRepository: PlaylistRepository = mockk()
    private val playingQueueRepository: PlayingQueueRepository = mockk()
    private val mediaLibraryManager: MediaLibraryManager = mockk()

    private val songsFlow = MutableStateFlow<List<SongDto>>(emptyList())
    private val artistsFlow = MutableStateFlow<List<ArtistDto>>(emptyList())
    private val albumsFlow = MutableStateFlow<List<AlbumDto>>(emptyList())
    private val genresFlow = MutableStateFlow<List<GenreDto>>(emptyList())
    private val playlistsFlow = MutableStateFlow<List<PlaylistDto>>(emptyList())

    private lateinit var viewModel: LibraryViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { musicRepository.getAllSongs() } returns songsFlow
        every { artistRepository.getArtistInformation() } returns artistsFlow
        every { albumRepository.getAlbumInformation() } returns albumsFlow
        every { genreRepository.getGenreInformation() } returns genresFlow
        every { playlistRepository.getPlaylistInformation() } returns playlistsFlow

        viewModel = LibraryViewModel(
            musicRepository,
            artistRepository,
            albumRepository,
            genreRepository,
            playlistRepository,
            playingQueueRepository,
            mediaLibraryManager,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState combines all repository flows`() = runTest {
        viewModel.uiState.test {
            // Initial state from flows
            assertThat(awaitItem()).isEqualTo(LibraryUiState())

            val artist = ArtistDto("Artist 1", 1, 10)
            artistsFlow.value = listOf(artist)
            assertThat(awaitItem()).isEqualTo(LibraryUiState(artists = listOf(artist)))

            val song = SongDto(id = "s1", title = "Song 1")
            songsFlow.value = listOf(song)
            assertThat(awaitItem()).isEqualTo(LibraryUiState(artists = listOf(artist), songs = listOf(song)))
        }
    }
}
