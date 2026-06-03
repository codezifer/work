package de.carsten.android.muzzic.persistence.entity.aggregation

import de.carsten.android.muzzic.model.AlbumArt

data class GenreAggregation(
    val genreName: String,
    val artistCount: Int,
    val albumCount: Int,
    val songCount: Int,
    val genreDuration: Long,
    override val lastAlbumArt: String? = null,
    val allAlbumArts: String? = null,
) : AlbumArt
