package de.carsten.android.muzzic.ui.model

import android.os.Bundle
import androidx.compose.runtime.Immutable
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import de.carsten.android.muzzic.EMPTY
import de.carsten.android.muzzic.UNKNOWN
import de.carsten.android.muzzic.inferMimeType
import de.carsten.android.muzzic.toPlayableUri

@Immutable
data class PlayingQueueDto(
    val title: String = UNKNOWN,
    val trackNumber: Int = -1,
    val totalTracks: Int = -1,
    val artist: String = UNKNOWN,
    val album: String = UNKNOWN,
    val albumYear: Int = -1,
    val albumArt: String = UNKNOWN,
    val genre: String = UNKNOWN,
    val duration: Long = -1,
    val filePath: String = EMPTY,
    val enqueued: Boolean = false,
    val queuePosition: Int = -1,
    val songId: String = EMPTY,
    val mediaId: String = EMPTY,
) : Comparable<PlayingQueueDto> {

    override fun compareTo(other: PlayingQueueDto): Int {
        val c1 = this.genre.compareTo(other.genre)
        return if (c1 != 0) {
            c1
        } else {
            val c2 = this.artist.compareTo(other.artist)
            if (c2 != 0) {
                c2
            } else {
                val c3 = this.album.compareTo(other.album)
                if (c3 != 0) {
                    c3
                } else {
                    this.title.compareTo(other.title)
                }
            }
        }
    }

    fun toMediaItem() = MediaItem
        .Builder()
        .setMediaId(mediaId)
        .setUri(filePath.toPlayableUri())
        .setMimeType(filePath.inferMimeType())
        .setMediaMetadata(
            MediaMetadata
                .Builder()
                .setTitle(title)
                .setTrackNumber(trackNumber)
                .setTotalTrackCount(totalTracks)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setReleaseYear(albumYear)
                .setGenre(genre)
                .setDurationMs(duration)
                .setArtworkUri(albumArt.toUri())
                .setExtras(
                    Bundle().apply {
                        putBoolean("enqueued", enqueued)
                        putInt("queuePosition", queuePosition)
                        putString("songId", songId)
                    },
                ).build(),
        ).build()
}
