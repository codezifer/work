package de.carsten.android.muzzic.ui.model

import androidx.compose.runtime.Immutable
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import de.carsten.android.muzzic.model.AlbumArt
import de.carsten.android.muzzic.model.MediaKeys
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

fun MediaItem.toGenreDto(): GenreDto {
    val metadata = mediaMetadata
    val extras = metadata.extras ?: android.os.Bundle.EMPTY
    return GenreDto(
        genreName = metadata.title?.toString() ?: "",
        artistCount = extras.getInt(MediaKeys.ARTIST_COUNT),
        albumCount = extras.getInt(MediaKeys.ALBUM_COUNT),
        songCount = extras.getInt(MediaKeys.SONG_COUNT),
        genreDuration = extras.getLong(MediaKeys.DURATION)
    )
}

fun GenreDto.toMediaItem(): MediaItem {
    return MediaItem.Builder()
        .setMediaId("${MediaKeys.GENRE_PREFIX}$genreName")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(genreName)
                .setArtworkUri(lastAlbumArt?.toUri())
                .setExtras(android.os.Bundle().apply {
                    putInt(MediaKeys.ARTIST_COUNT, artistCount)
                    putInt(MediaKeys.ALBUM_COUNT, albumCount)
                    putInt(MediaKeys.SONG_COUNT, songCount)
                    putLong(MediaKeys.DURATION, genreDuration)
                })
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .build()
        )
        .build()
}
