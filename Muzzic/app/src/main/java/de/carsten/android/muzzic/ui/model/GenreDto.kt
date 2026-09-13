package de.carsten.android.muzzic.ui.model

import androidx.compose.runtime.Immutable
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import de.carsten.android.muzzic.model.AlbumArt
import de.carsten.android.muzzic.model.MediaKeys
import de.carsten.android.muzzic.persistence.entity.aggregation.CanonicalGenreAggregation
import de.carsten.android.muzzic.persistence.entity.aggregation.GenreAggregation

@Immutable
data class GenreDto(
    val genreName: String,
    val artistCount: Int,
    val albumCount: Int,
    val songCount: Int,
    val genreDuration: Long,
    override val lastAlbumArt: String? = null,
    override val albumArts: List<String> = emptyList(),
) : AlbumArt,
    Comparable<GenreDto> {

    override fun compareTo(other: GenreDto): Int = this.genreName.compareTo(other.genreName)

    fun toMediaItem() = MediaItem
        .Builder()
        .setMediaId("${MediaKeys.GENRE_PREFIX}$genreName")
        .setMediaMetadata(
            MediaMetadata
                .Builder()
                .setTitle(genreName)
                .setArtworkUri(lastAlbumArt?.toUri())
                .setExtras(
                    android.os.Bundle().apply {
                        putInt(MediaKeys.ARTIST_COUNT, artistCount)
                        putInt(MediaKeys.ALBUM_COUNT, albumCount)
                        putInt(MediaKeys.SONG_COUNT, songCount)
                        putLong(MediaKeys.DURATION, genreDuration)
                    },
                ).setIsBrowsable(true)
                .setIsPlayable(false)
                .build(),
        ).build()
}

fun GenreAggregation.toDto() = GenreDto(
    genreName = this.genreName,
    artistCount = this.artistCount,
    albumCount = this.albumCount,
    songCount = this.songCount,
    genreDuration = this.genreDuration,
    lastAlbumArt = this.lastAlbumArt,
    albumArts = this.allAlbumArts?.split(",") ?: listOfNotNull(lastAlbumArt),
)

fun List<GenreAggregation>.toDto() = map { it.toDto() }

fun CanonicalGenreAggregation.toDto(canonicalName: String) = GenreDto(
    genreName = canonicalName,
    artistCount = this.artistCount,
    albumCount = this.albumCount,
    songCount = this.songCount,
    genreDuration = this.genreDuration,
    lastAlbumArt = this.lastAlbumArt,
    albumArts = this.allAlbumArts?.split(",") ?: listOfNotNull(lastAlbumArt),
)

fun MediaItem.toGenreDto(): GenreDto {
    val metadata = mediaMetadata
    val extras = metadata.extras ?: android.os.Bundle.EMPTY
    return GenreDto(
        genreName = metadata.title?.toString() ?: "",
        artistCount = extras.getInt(MediaKeys.ARTIST_COUNT),
        albumCount = extras.getInt(MediaKeys.ALBUM_COUNT),
        songCount = extras.getInt(MediaKeys.SONG_COUNT),
        genreDuration = extras.getLong(MediaKeys.DURATION),
    )
}
