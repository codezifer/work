package de.carsten.android.muzzic.persistence.repo

import de.carsten.android.muzzic.persistence.entity.Song
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * Unit tests for genre variant resolution in [ArtistRepository].
 */
class ArtistRepositoryTest {

    private val songs = listOf(
        Song(title = "A", artist = "X", album = "M", genre = "Death Core", duration = 100L),
        Song(title = "B", artist = "X", album = "M", genre = "Death-Core", duration = 200L),
        Song(title = "C", artist = "Y", album = "N", genre = "Deathcore", duration = 300L),
        Song(title = "D", artist = "Z", album = "O", genre = "Rock", duration = 400L),
    )
    private val artistDao = FakeArtistDao(songs)
    private val repository = ArtistRepository(artistDao, FakeGenreDao(songs))

    @Test
    fun `getArtistsByGenre resolves canonical name across variants`() = runTest {
        val artists = repository.getArtistsByGenre("Death Core").first()

        assertThat(artists.map { it.artistName }).containsExactly("X", "Y")
        assertThat(artists.first { it.artistName == "X" }.songCount).isEqualTo(2)
        assertThat(artists.first { it.artistName == "Y" }.songCount).isEqualTo(1)
        assertThat(artistDao.genresCalls.single())
            .containsExactlyInAnyOrder("Death Core", "Death-Core", "Deathcore")
    }

    @Test
    fun `getArtistsByGenre resolves stored variant`() = runTest {
        val artists = repository.getArtistsByGenre("Deathcore").first()

        assertThat(artists.map { it.artistName }).containsExactly("X", "Y")
    }

    @Test
    fun `getArtistsByGenre returns empty without query for unknown genre`() = runTest {
        assertThat(repository.getArtistsByGenre("Polka").first()).isEmpty()
        assertThat(artistDao.genresCalls).isEmpty()
    }

    @Test
    fun `getRecentArtists orders by addition recency and applies limit`() = runTest {
        val timed = { artist: String, epochMilli: Long ->
            Song(title = "T", artist = artist, album = "M").apply { createdAt = java.time.Instant.ofEpochMilli(epochMilli) }
        }
        val timedRepository = ArtistRepository(
            FakeArtistDao(listOf(timed("Old", 1000L), timed("New", 3000L), timed("Mid", 2000L))),
            FakeGenreDao(emptyList()),
        )

        assertThat(timedRepository.getRecentArtists(limit = 2).map { it.artistName })
            .containsExactly("New", "Mid")
    }
}
