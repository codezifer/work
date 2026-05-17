package de.carsten.android.muzzic.ui.model

import android.os.Bundle
import androidx.compose.runtime.Immutable
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import de.carsten.android.muzzic.EMPTY
import de.carsten.android.muzzic.UNKNOWN

@Immutable
data class PlayingQueueDto(
    val title: String = UNKNOWN,
    val trackNumber: Int = -1,
    val totalTracks: Int = -1,
    val artist: String = UNKNOWN,
    val album: String = UNKNOWN,
    val albumArt: String = UNKNOWN,
    val genre: String = UNKNOWN,
    val duration: Long = -1,
    val filePath: String = EMPTY,
    val enqueued: Boolean = false,
    val queuePosition: Int = -1,
    val songId: String = EMPTY,
    val mediaId: String = EMPTY
) {
    fun toMediaItem() = MediaItem.Builder()
        .setMediaId(mediaId)
        .setUri(filePath)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setTrackNumber(trackNumber)
                .setTotalTrackCount(totalTracks)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setGenre(genre)
                .setDurationMs(duration)
                .setArtworkUri(albumArt.toUri())
                .setExtras(Bundle().apply {
                    putBoolean("enqueued", enqueued)
                    putInt("queuePosition", queuePosition)
                    putString("songId", songId)
                })
                .build()
        )
        .build()
}
