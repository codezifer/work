package de.carsten.android.muzzic.persistence.entity.aggregation

import de.carsten.android.muzzic.model.AlbumArt

data class ArtistAggregation(
    val artistName: String,
    val albumCount: Int,
    val songCount: Int,
    override val lastAlbumArt: String? = null,
) : AlbumArt
