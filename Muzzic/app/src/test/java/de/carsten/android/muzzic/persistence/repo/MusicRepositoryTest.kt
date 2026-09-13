package de.carsten.android.muzzic.persistence.repo

import android.content.Context
import de.carsten.android.muzzic.persistence.MuzzicDatabase
import de.carsten.android.muzzic.persistence.dao.PlayHistoryDao
import de.carsten.android.muzzic.persistence.dao.PlayingQueueDao
import de.carsten.android.muzzic.persistence.dao.PlaylistDao
import de.carsten.android.muzzic.persistence.dao.SongDao
import de.carsten.android.muzzic.persistence.entity.Song
import de.carsten.android.muzzic.persistence.entity.aggregation.GenrePlayCount
import de.carsten.android.muzzic.scanning.MusicFileScanner
import de.carsten.android.muzzic.scanning.PlaylistFileScanner
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * Unit tests for canonical genre statistics in [MusicRepository].
 */
class MusicRepositoryTest {

    private val songs = listOf(
        Song(title = "A", artist = "X", album = "M", genre = "Death Core", duration = 100L),
        Song(title = "B", artist = "X", album = "M", genre = "Death-Core", duration = 200L),
        Song(title = "C", artist = "Y", album = "N", genre = "Deathcore", duration = 300L),
    )
    private val songDao: SongDao = mockk()
    private val playHistoryDao: PlayHistoryDao = mockk()
    private val repository = MusicRepository(
        songDao = songDao,
        playHistoryDao = playHistoryDao,
        playlistDao = mockk<PlaylistDao>(),
        playingQueueDao = mockk<PlayingQueueDao>(),
        database = mockk<MuzzicDatabase>(),
        context = mockk<Context>(),
        musicFileScanner = mockk<MusicFileScanner>(),
        playlistFileScanner = mockk<PlaylistFileScanner>(),
        genreRepository = GenreRepository(FakeGenreDao(songs)),
    )

    @Test
    fun `getGenreStats merges variants before applying the limit`() = runTest {
        val raw = listOf(GenrePlayCount("deathcore", 100)) +
            (1..10).map { GenrePlayCount("genre$it", 90 - it) }
        coEvery { songDao.getGenreStats(any(), any()) } returns raw

        val result = repository.getGenreStats()

        assertThat(result).hasSize(10)
        assertThat(result.first().genre).isEqualTo("Death Core")
        assertThat(result.map { it.count }).isSortedAccordingTo(Comparator.reverseOrder())
    }

    @Test
    fun `getMonthGenreStats resolves canonical names`() = runTest {
        coEvery { playHistoryDao.getGenreStats(any()) } returns listOf(
            GenrePlayCount("deathcore", 12),
            GenrePlayCount("mystery", 3),
        )

        val result = repository.getMonthGenreStats()

        assertThat(result.map { it.genre }).containsExactly("Death Core", "mystery")
    }
}
