package de.carsten.android.muzzic.ui.model

import androidx.compose.runtime.Immutable
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import de.carsten.android.muzzic.model.AlbumArt
import de.carsten.android.muzzic.model.MediaKeys
import de.carsten.android.muzzic.persistence.entity.aggregation.ArtistAggregation

@Immutable
data class ArtistDto(
    val artistName: String,
    val albumCount: Int,
    val songCount: Int,
    override val lastAlbumArt: String? = null,
) : AlbumArt

fun ArtistAggregation.toDto() = ArtistDto(
    artistName = this.artistName,
    albumCount = this.albumCount,
    songCount = this.songCount,
    lastAlbumArt = this.lastAlbumArt,
)

fun List<ArtistAggregation>.toDto() = map { it.toDto() }

fun MediaItem.toArtistDto(): ArtistDto {
    val metadata = mediaMetadata
    val extras = metadata.extras ?: android.os.Bundle.EMPTY
    return ArtistDto(
        artistName = metadata.title?.toString() ?: "",
        albumCount = extras.getInt(MediaKeys.ALBUM_COUNT),
        songCount = extras.getInt(MediaKeys.SONG_COUNT),
        lastAlbumArt = metadata.artworkUri?.toString()
    )
}

fun ArtistDto.toMediaItem(): MediaItem {
    return MediaItem.Builder()
        .setMediaId("${MediaKeys.ARTIST_PREFIX}$artistName")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(artistName)
                .setArtworkUri(lastAlbumArt?.toUri())
                .setExtras(android.os.Bundle().apply {
                    putInt(MediaKeys.ALBUM_COUNT, albumCount)
                    putInt(MediaKeys.SONG_COUNT, songCount)
                })
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .build()
        )
        .build()
}
