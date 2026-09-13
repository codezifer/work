package de.carsten.android.muzzic.persistence.entity.aggregation

import de.carsten.android.muzzic.model.AlbumArt

/**
 * Song statistics grouped by normalized genre key.
 *
 * The [normalizedKey] mirrors [de.carsten.android.muzzic.utils.GenreUtils.normalizeKey],
 * so spelling variants like "Death Core", "Death-Core" and "Deathcore" share one row.
 * Counts are exact across variants (distinct artists/albums, summed songs/duration).
 */
data class CanonicalGenreAggregation(
    val normalizedKey: String,
    val artistCount: Int,
    val albumCount: Int,
    val songCount: Int,
    val genreDuration: Long,
    override val lastAlbumArt: String? = null,
    val allAlbumArts: String? = null,
) : AlbumArt
