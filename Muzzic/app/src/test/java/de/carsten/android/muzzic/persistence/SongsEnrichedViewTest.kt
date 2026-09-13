package de.carsten.android.muzzic.persistence

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import de.carsten.android.muzzic.TestConfig
import de.carsten.android.muzzic.persistence.entity.Song
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies that the songs_enriched view mirrors GenreUtils semantics in real SQL:
 * genre variants share one normalized key with exact counts, and artists sort
 * article-aware.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestConfig.SDK])
class SongsEnrichedViewTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: MuzzicDatabase

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(context, MuzzicDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `view groups genre variants with exact counts`() = runTest {
        db.songDao().insertSongs(
            listOf(
                Song(title = "A", artist = "X", album = "M", genre = "Death Core", duration = 100L),
                Song(title = "B", artist = "X", album = "M", genre = "Death-Core", duration = 200L),
                Song(title = "C", artist = "Y", album = "N", genre = "Deathcore", duration = 300L),
                Song(title = "D", artist = "Z", album = "O", genre = "Pop", duration = 400L),
            ),
        )

        val canonical = db.genreDao().getCanonicalGenreAggregations().first()

        assertThat(canonical.map { it.normalizedKey }).containsExactly("deathcore", "pop")
        val deathcore = canonical.first { it.normalizedKey == "deathcore" }
        assertThat(deathcore.songCount).isEqualTo(3)
        assertThat(deathcore.artistCount).isEqualTo(2)
        assertThat(deathcore.albumCount).isEqualTo(2)
        assertThat(deathcore.genreDuration).isEqualTo(600L)
    }

    @Test
    fun `view sorts artists article-aware`() = runTest {
        db.songDao().insertSongs(
            listOf(
                Song(title = "A", artist = "The Beatles", album = "M", genre = "Rock"),
                Song(title = "B", artist = "Bob Dylan", album = "N", genre = "Rock"),
                Song(title = "C", artist = "ABBA", album = "O", genre = "Pop"),
            ),
        )

        // Raw ordering would be ABBA, Bob Dylan, The Beatles.
        val artists = db.artistDao().getArtistAggregations().first()

        assertThat(artists.map { it.artistName }).containsExactly("ABBA", "The Beatles", "Bob Dylan")
    }

    @Test
    fun `view aggregates variant songs for stats`() = runTest {
        db.songDao().insertSongs(
            listOf(
                Song(title = "A", artist = "X", album = "M", genre = "Death Core", playCount = 5),
                Song(title = "B", artist = "Y", album = "N", genre = "Deathcore", playCount = 7),
            ),
        )

        val stats = db.songDao().getGenreStats(fromTimestamp = 0L, unknownLabel = "Unknown")

        assertThat(stats.map { it.genre }).containsExactly("deathcore")
        assertThat(stats.single().count).isEqualTo(12)
    }
}
