package de.carsten.android.muzzic.persistence.repo

import de.carsten.android.muzzic.persistence.dao.GenreDao
import de.carsten.android.muzzic.persistence.entity.aggregation.GenrePlayCount
import de.carsten.android.muzzic.ui.model.GenreDto
import de.carsten.android.muzzic.ui.model.SongDto
import de.carsten.android.muzzic.ui.model.toDto
import de.carsten.android.muzzic.utils.GenreUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GenreRepository(val genreDao: GenreDao) {
    /**
     * Returns songs of the given genre, including all spelling variants.
     *
     * Both canonical names (e.g. "Death Core") and stored variants (e.g. "Deathcore")
     * resolve to the same variant group. Unknown names yield an empty list.
     *
     * @param genre canonical genre name or stored genre variant.
     * @return songs of all resolved variants.
     */
    suspend fun getSongsByGenre(genre: String): List<SongDto> {
        val variants = getGenreVariants(genre)
        if (variants.isEmpty()) return emptyList()
        return genreDao.getSongsByGenres(variants).map { song -> song.toDto() }
    }

    /**
     * Resolves a canonical genre name or stored variant to all stored spelling variants.
     *
     * @param genre canonical genre name or stored genre variant.
     * @return stored variant names sharing the normalized key, or empty if unknown.
     */
    suspend fun getGenreVariants(genre: String): List<String> {
        val key = GenreUtils.normalizeKey(genre)
        return genreDao.getGenreCounts().map { it.genre }.filter { GenreUtils.normalizeKey(it) == key }
    }

    /**
     * Maps genre statistics keyed by normalized genre to canonical display names.
     *
     * Unknown keys without stored variants (e.g. the blank-genre label) fall back to
     * themselves. Counts of merged variants are already summed per key by the query.
     *
     * @param rawStats statistics with normalized genre keys (see `getGenreStats` queries).
     * @return statistics with canonical genre names, ordered by count descending.
     */
    suspend fun getCanonicalGenreStats(rawStats: List<GenrePlayCount>): List<GenrePlayCount> {
        val weights = GenreUtils.groupByNormalizedKey(genreDao.getGenreCounts().associate { it.genre to it.count })
        return rawStats
            .map { stat ->
                val variants = weights[GenreUtils.normalizeKey(stat.genre)]
                GenrePlayCount(variants?.let { GenreUtils.getCanonicalName(it) } ?: stat.genre, stat.count)
            }.sorted()
    }

    /**
     * Emits one [GenreDto] per canonical genre.
     *
     * Spelling variants sharing a normalized key are merged into a single entry named
     * after the canonical variant, with exact distinct artist/album counts and summed
     * song counts and durations.
     *
     * @return canonical genres ordered by normalized key.
     */
    fun getGenreInformation(): Flow<List<GenreDto>> = combine(
        genreDao.getGenreAggregations(),
        genreDao.getCanonicalGenreAggregations(),
    ) { variantAggregations, canonicalAggregations ->
        val weightsByKey = variantAggregations
            .groupBy({ GenreUtils.normalizeKey(it.genreName) }) { it }
            .mapValues { (_, aggregations) -> aggregations.associate { it.genreName to it.songCount } }
        canonicalAggregations.mapNotNull { canonical ->
            val variants = weightsByKey[canonical.normalizedKey] ?: return@mapNotNull null
            canonical.toDto(GenreUtils.getCanonicalName(variants))
        }
    }
}
