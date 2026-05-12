package de.carsten.android.muzzic.viewmodel

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import de.carsten.android.muzzic.persistence.entity.Song
import de.carsten.android.muzzic.persistence.repo.AlbumRepository
import de.carsten.android.muzzic.persistence.repo.ArtistRepository
import de.carsten.android.muzzic.persistence.repo.PlayingQueueRepository
import de.carsten.android.muzzic.persistence.repo.SongRepository
import de.carsten.android.muzzic.viewmodel.states.SelectionState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SelectionViewModelTest {

    private val artistRepository: ArtistRepository = mockk()
    private val albumRepository: AlbumRepository = mockk()
    private val songRepository: SongRepository = mockk()
    private val playingQueueRepository: PlayingQueueRepository = mockk()

    private lateinit var viewModel: SelectionViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = SelectionViewModel(
            artistRepository,
            albumRepository,
            songRepository,
            playingQueueRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `toggleArtistSelection updates state correctly`() = runTest {
        val song1 = createSong("1", "Artist 1", "Album 1")
        coEvery { artistRepository.getSongsByArtist("Artist 1") } returns listOf(song1)

        viewModel.selectionState.test {
            assertEquals(SelectionState(), awaitItem())

            viewModel.toggleArtistSelection("Artist 1")
            testScheduler.advanceUntilIdle()

            val state1 = awaitItem()
            assertTrue(state1.isActive)
            assertTrue(state1.selectedArtists.contains("Artist 1"))
            assertTrue(state1.selectedSongs.contains("1"))

            viewModel.toggleArtistSelection("Artist 1")
            testScheduler.advanceUntilIdle()

            val state2 = awaitItem()
            assertFalse(state2.isActive)
            assertFalse(state2.selectedArtists.contains("Artist 1"))
            assertFalse(state2.selectedSongs.contains("1"))
        }
    }

    @Test
    fun `toggleAlbumSelection updates state correctly`() = runTest {
        val song1 = createSong("1", "Artist 1", "Album 1")
        every { albumRepository.getSongsByAlbum("Artist 1", "Album 1") } returns flowOf(listOf(song1))

        viewModel.selectionState.test {
            awaitItem() // Initial state

            viewModel.toggleAlbumSelection("Artist 1", "Album 1")
            testScheduler.advanceUntilIdle()

            val state = awaitItem()
            assertTrue(state.isActive)
            assertTrue(state.selectedAlbums.contains("Artist 1|Album 1"))
            assertTrue(state.selectedSongs.contains("1"))
        }
    }

    @Test
    fun `toggleSongSelection updates state correctly`() = runTest {
        viewModel.selectionState.test {
            awaitItem() // Initial state

            viewModel.toggleSongSelection("song_123")
            val state = awaitItem()
            assertTrue(state.isActive)
            assertTrue(state.selectedSongs.contains("song_123"))
        }
    }

    @Test
    fun `clearSelection resets state`() = runTest {
        val song1 = createSong("1", "Artist 1", "Album 1")
        coEvery { artistRepository.getSongsByArtist("Artist 1") } returns listOf(song1)

        viewModel.toggleArtistSelection("Artist 1")
        testScheduler.advanceUntilIdle()

        viewModel.selectionState.test {
            assertTrue(awaitItem().isActive)

            viewModel.clearSelection()
            assertFalse(awaitItem().isActive)
        }
    }

    @Test
    fun `confirmSelection resolves all and adds to queue`() = runTest {
        val song1 = createSong("1", "Artist 1", "Album 1")
        val song2 = createSong("2", "Artist 2", "Album 2")
        val song3 = createSong("3", "Artist 3", "Album 3")

        coEvery { artistRepository.getSongsByArtist("Artist 1") } returns listOf(song1)
        every { albumRepository.getSongsByAlbum("Artist 2", "Album 2") } returns flowOf(listOf(song2))
        coEvery { songRepository.getSongsByIds(*anyVararg()) } returns listOf(song1, song2, song3)
        coEvery { playingQueueRepository.addSongs(any(), any()) } returns emptyList()

        viewModel.toggleArtistSelection("Artist 1")
        testScheduler.advanceUntilIdle()
        viewModel.toggleAlbumSelection("Artist 2", "Album 2")
        testScheduler.advanceUntilIdle()
        viewModel.toggleSongSelection("3")

        viewModel.confirmSelection { }
        testScheduler.advanceUntilIdle()

        coVerify {
            playingQueueRepository.addSongs(any())
        }

        // Selection is ENQUEUED after confirmation
        assertTrue(viewModel.selectionState.value.isActive)
        assertEquals(de.carsten.android.muzzic.viewmodel.states.Selection.ENQUEUED, viewModel.selectionState.value.value)
    }

    @Test
    fun `rollbackEnqueued removes recently added songs`() = runTest {
        val song1 = createSong("1", "Artist 1", "Album 1")
        coEvery { songRepository.getSongsByIds(*anyVararg()) } returns listOf(song1)
        coEvery { playingQueueRepository.addSongs(any(), any()) } returns emptyList()
        coEvery { playingQueueRepository.removeSongs(any()) } returns Unit

        viewModel.toggleSongSelection("1")
        viewModel.confirmSelection { }
        testScheduler.advanceUntilIdle()

        viewModel.rollbackEnqueued { }
        testScheduler.advanceUntilIdle()

        coVerify {
            playingQueueRepository.removeSongs(any())
        }
        assertFalse(viewModel.selectionState.value.isActive)
    }

    private fun createSong(id: String, artist: String, album: String) = Song(
        title = "Title $id",
        artist = artist,
        album = album,
        filePath = "path/$id",
        duration = 1000,
        trackNumber = 1,
        totalTracks = 10,
        genre = "Genre",
        rating = 0,
        playCount = 0,
        lastPlayed = Instant.now()
    ).apply { this.id = id }
}
