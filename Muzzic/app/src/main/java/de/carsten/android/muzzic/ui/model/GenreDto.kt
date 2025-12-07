package de.carsten.android.muzzic.ui.model

import androidx.compose.runtime.Immutable
import de.carsten.android.muzzic.model.AlbumArt
import de.carsten.android.muzzic.persistence.entity.aggregation.GenreAggregation

@Immutable
data class GenreDto(
    val genreName: String,
    val artistCount: Int,
    val albumCount: Int,
    val songCount: Int,
    val genreDuration: Long,
    override val lastAlbumArt: String? = null,
) : AlbumArt

fun GenreAggregation.toDto() =
    GenreDto(
        genreName = this.genreName,
        artistCount = this.artistCount,
        albumCount = this.albumCount,
        songCount = this.songCount,
        genreDuration = this.genreDuration,
        lastAlbumArt = this.lastAlbumArt,
    )

fun List<GenreAggregation>.toDto() = map { it.toDto() }
