package de.carsten.android.muzzic.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.media3.common.util.UnstableApi
import app.cash.turbine.test
import de.carsten.android.muzzic.persistence.repo.PlayingQueueRepository
import de.carsten.android.muzzic.persistence.repo.PlaylistRepository
import de.carsten.android.muzzic.service.MediaLibraryManager
import de.carsten.android.muzzic.ui.AppDestinations.PLAYLIST_ID_ARGUMENT
import de.carsten.android.muzzic.ui.model.SongDto
import de.carsten.android.muzzic.ui.state.PlaylistSongsUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

@UnstableApi
@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistSongsViewModelTest {
    private val playlistId = "playlist-1"
    private val playlistRepository: PlaylistRepository = mockk()
    private val playingQueueRepository: PlayingQueueRepository = mockk()
    private val mediaLibraryManager: MediaLibraryManager = mockk()
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(mapOf(PLAYLIST_ID_ARGUMENT to playlistId))

    private val playlistNameFlow = MutableStateFlow<String?>("Rock Anthems")
    private val songsFlow = MutableStateFlow(emptyList<SongDto>())
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: PlaylistSongsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { playlistRepository.observePlaylistName(playlistId) } returns playlistNameFlow
        every { playlistRepository.observeSongsInPlaylist(playlistId) } returns songsFlow

        viewModel = PlaylistSongsViewModel(savedStateHandle, playlistRepository, playingQueueRepository, mediaLibraryManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState combines playlist name songs and total duration`() = runTest {
        viewModel.uiState.test {
            assertThat(awaitItem()).isEqualTo(PlaylistSongsUiState(playlistId = playlistId))

            val songs =
                listOf(
                    SongDto(id = "1", title = "Song 1", duration = 60_000L),
                    SongDto(id = "2", title = "Song 2", duration = 120_000L),
                )
            songsFlow.value = songs

            assertThat(awaitItem()).isEqualTo(
                PlaylistSongsUiState(
                    playlistId = playlistId,
                    playlistName = "Rock Anthems",
                    songs = songs,
                    totalDurationMs = 180_000L,
                ),
            )

            playlistNameFlow.value = "Renamed"

            assertThat(awaitItem()).isEqualTo(
                PlaylistSongsUiState(
                    playlistId = playlistId,
                    playlistName = "Renamed",
                    songs = songs,
                    totalDurationMs = 180_000L,
                ),
            )
        }
    }

    @Test
    fun `playSong replaces the queue and starts at the selected song`() = runTest {
        val songs =
            listOf(
                SongDto(id = "1", title = "Song 1", duration = 60_000L, filePath = "/tmp/song1.mp3"),
                SongDto(id = "2", title = "Song 2", duration = 120_000L, filePath = "/tmp/song2.mp3"),
            )
        coEvery { playingQueueRepository.clear() } returns Unit
        coEvery { playingQueueRepository.addSongs(any()) } returns emptyList()
        every { mediaLibraryManager.playPlaylist(any(), any()) } returns Unit

        viewModel.playSong(songs[1], songs)
        advanceUntilIdle()

        coVerify { playingQueueRepository.clear() }
        coVerify { playingQueueRepository.addSongs(match { it.size == songs.size }) }
        verify { mediaLibraryManager.playPlaylist(any(), 1) }
    }

    @Test
    fun `playSong does nothing when the playlist is empty`() = runTest {
        viewModel.playSong(SongDto(id = "1"), emptyList())
        advanceUntilIdle()

        coVerify(exactly = 0) { playingQueueRepository.clear() }
        verify(exactly = 0) { mediaLibraryManager.playPlaylist(any(), any()) }
    }
}
