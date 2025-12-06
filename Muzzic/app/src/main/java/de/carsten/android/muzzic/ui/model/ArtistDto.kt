package de.carsten.android.muzzic.ui.model

import androidx.compose.runtime.Immutable
import de.carsten.android.muzzic.model.AlbumArt
import de.carsten.android.muzzic.persistence.entity.aggregation.ArtistAggregation

@Immutable
data class ArtistDto(
    val artistName: String,
    val albumCount: Int,
    val songCount: Int,
    override val lastAlbumArt: String? = null,
): AlbumArt

fun ArtistAggregation.toDto() = ArtistDto(
    artistName = this.artistName,
    albumCount = this.albumCount,
    songCount = this.songCount,
    lastAlbumArt = this.lastAlbumArt,
)

fun List<ArtistAggregation>.toDto() = map { it.toDto() }
