package de.carsten.android.muzzic.persistence.repo

import de.carsten.android.muzzic.persistence.dao.GenreDao
import de.carsten.android.muzzic.persistence.entity.Song
import de.carsten.android.muzzic.persistence.entity.aggregation.CanonicalGenreAggregation
import de.carsten.android.muzzic.persistence.entity.aggregation.GenreAggregation
import de.carsten.android.muzzic.persistence.entity.aggregation.GenrePlayCount
import de.carsten.android.muzzic.utils.GenreUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * In-memory [GenreDao] for repository unit tests.
 *
 * Mirrors the SQL semantics of the Room queries: per-variant groupings and
 * canonical groupings by [GenreUtils.normalizeKey].
 *
 * @param songs backing songs.
 */
class FakeGenreDao(private val songs: List<Song>) : GenreDao {

    /** Variant lists passed to [getSongsByGenres], in call order. */
    val songsByGenresCalls = mutableListOf<List<String>>()

    override suspend fun getSongsByGenres(genres: List<String>): List<Song> {
        songsByGenresCalls.add(genres)
        return songs.filter { it.genre in genres }.sortedBy { it.title }
    }

    override suspend fun getTopSongsByGenre(genre: String, limit: Int): List<Song> = songs
        .filter { it.genre == genre }
        .sortedWith(compareByDescending<Song> { it.rating }.thenByDescending { it.playCount })
        .take(limit)

    override suspend fun getGenreCounts(): List<GenrePlayCount> = songs
        .groupBy { it.genre }
        .map { (genre, grouped) -> GenrePlayCount(genre, grouped.size) }

    override fun getGenreAggregations(): Flow<List<GenreAggregation>> = flowOf(
        songs.groupBy { it.genre }.map { (genre, grouped) ->
            GenreAggregation(
                genreName = genre,
                artistCount = grouped.map { it.artist }.distinct().size,
                albumCount = grouped.map { it.album }.distinct().size,
                songCount = grouped.size,
                genreDuration = grouped.sumOf { it.duration },
            )
        }.sortedBy { it.genreName },
    )

    override fun getCanonicalGenreAggregations(): Flow<List<CanonicalGenreAggregation>> = flowOf(
        songs.groupBy { GenreUtils.normalizeKey(it.genre) }.map { (key, grouped) ->
            CanonicalGenreAggregation(
                normalizedKey = key,
                artistCount = grouped.map { it.artist }.distinct().size,
                albumCount = grouped.map { it.album }.distinct().size,
                songCount = grouped.size,
                genreDuration = grouped.sumOf { it.duration },
            )
        }.sortedBy { it.normalizedKey },
    )
}
