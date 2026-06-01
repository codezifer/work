package de.carsten.android.muzzic.ui.model

import androidx.compose.runtime.Immutable
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import de.carsten.android.muzzic.model.AlbumArt
import de.carsten.android.muzzic.model.MediaKeys
import de.carsten.android.muzzic.persistence.entity.aggregation.AlbumAggregation

@Immutable
data class AlbumDto(val albumName: String, val albumYear: Int, val artistName: String, val songCount: Int, val albumDuration: Long, override val lastAlbumArt: String? = null) :
    AlbumArt

fun AlbumAggregation.toDto() = AlbumDto(
    albumName = this.albumName,
    albumYear = this.albumYear,
    artistName = this.artistName,
    songCount = this.songCount,
    albumDuration = this.albumDuration,
    lastAlbumArt = this.lastAlbumArt,
)

fun List<AlbumAggregation>.toDto() = map { it.toDto() }

fun MediaItem.toAlbumDto(): AlbumDto {
    val metadata = mediaMetadata
    val extras = metadata.extras ?: android.os.Bundle.EMPTY
    return AlbumDto(
        artistName = metadata.artist?.toString() ?: "",
        albumName = metadata.title?.toString() ?: "",
        albumYear = metadata.releaseYear ?: 0,
        songCount = extras.getInt(MediaKeys.SONG_COUNT),
        albumDuration = extras.getLong(MediaKeys.DURATION),
        lastAlbumArt = metadata.artworkUri?.toString(),
    )
}

fun AlbumDto.toMediaItem(): MediaItem = MediaItem
    .Builder()
    .setMediaId("${MediaKeys.ALBUM_PREFIX}$artistName:$albumName")
    .setMediaMetadata(
        MediaMetadata
            .Builder()
            .setTitle(albumName)
            .setArtist(artistName)
            .setReleaseYear(albumYear)
            .setArtworkUri(lastAlbumArt?.toUri())
            .setExtras(
                android.os.Bundle().apply {
                    putInt(MediaKeys.SONG_COUNT, songCount)
                    putLong(MediaKeys.DURATION, albumDuration)
                },
            ).setIsBrowsable(true)
            .setIsPlayable(false)
            .build(),
    ).build()
