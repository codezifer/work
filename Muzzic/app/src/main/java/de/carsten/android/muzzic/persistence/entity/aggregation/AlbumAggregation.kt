package de.carsten.android.muzzic.persistence.entity.aggregation

import de.carsten.android.muzzic.model.AlbumArt

data class AlbumAggregation(
    val albumName: String,
    val artistName: String,
    val songCount: Int,
    val albumDuration: Long,
    val albumYear: Int,
    override val lastAlbumArt: String? = null,
): AlbumArt
