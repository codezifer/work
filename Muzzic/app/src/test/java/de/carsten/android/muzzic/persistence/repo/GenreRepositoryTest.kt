package de.carsten.android.muzzic.persistence.repo

import de.carsten.android.muzzic.persistence.entity.Song
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * Unit tests for genre variant merging in [GenreRepository].
 */
class GenreRepositoryTest {

    private val songs = listOf(
        Song(title = "A", artist = "X", album = "M", genre = "Death Core", duration = 100L),
        Song(title = "B", artist = "X", album = "M", genre = "Death-Core", duration = 200L),
        Song(title = "C", artist = "Y", album = "N", genre = "Deathcore", duration = 300L),
        Song(title = "D", artist = "Z", album = "O", genre = "Rock", duration = 400L),
    )
    private val genreDao = FakeGenreDao(songs)
    private val repository = GenreRepository(genreDao)

    @Test
    fun `getGenreInformation merges spelling variants into canonical genre`() = runTest {
        val genres = repository.getGenreInformation().first()

        assertThat(genres.map { it.genreName }).containsExactly("Death Core", "Rock")
        val deathCore = genres.first { it.genreName == "Death Core" }
        assertThat(deathCore.songCount).isEqualTo(3)
        assertThat(deathCore.genreDuration).isEqualTo(600L)
        assertThat(deathCore.artistCount).isEqualTo(2)
        assertThat(deathCore.albumCount).isEqualTo(2)
    }

    @Test
    fun `getSongsByGenre resolves canonical name to all variants`() = runTest {
        val result = repository.getSongsByGenre("Death Core")

        assertThat(result.map { it.title }).containsExactly("A", "B", "C")
        assertThat(genreDao.songsByGenresCalls.single())
            .containsExactlyInAnyOrder("Death Core", "Death-Core", "Deathcore")
    }

    @Test
    fun `getSongsByGenre resolves stored variant to all variants`() = runTest {
        val result = repository.getSongsByGenre("Deathcore")

        assertThat(result.map { it.title }).containsExactly("A", "B", "C")
    }

    @Test
    fun `getSongsByGenre returns empty without query for unknown genre`() = runTest {
        assertThat(repository.getSongsByGenre("Polka")).isEmpty()
        assertThat(genreDao.songsByGenresCalls).isEmpty()
    }

    @Test
    fun `getGenreVariants resolves canonical and variant names`() = runTest {
        val expected = listOf("Death Core", "Death-Core", "Deathcore")

        assertThat(repository.getGenreVariants("Death Core")).containsExactlyInAnyOrderElementsOf(expected)
        assertThat(repository.getGenreVariants("Death-Core")).containsExactlyInAnyOrderElementsOf(expected)
        assertThat(repository.getGenreVariants("Polka")).isEmpty()
    }
}
