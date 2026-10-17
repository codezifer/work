package de.carsten.android.muzzic.persistence.entity.aggregation

import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.room.Embedded
import de.carsten.android.muzzic.inferMimeType
import de.carsten.android.muzzic.model.MediaKeys
import de.carsten.android.muzzic.persistence.entity.Song
import de.carsten.android.muzzic.toPlayableUri
import de.carsten.android.muzzic.ui.model.SongDto
import de.carsten.android.muzzic.ui.utils.getStarRating

/**
 * Read model combining a `songs` row with its `playing_queue` state.
 *
 * Built by joining `playing_queue` with `songs` on the queue's song id, so the
 * `songs` table is the single source of truth for every playable value (title,
 * artist, rating, play count, …). The join is an `INNER JOIN`: a queue entry
 * whose song was removed is dropped by the foreign key cascade.
 *
 * @property song the library song, carrying all metadata and playback values.
 * @property enqueued whether the entry belongs to the active playback queue.
 * @property queuePosition the position inside the queue.
 */
data class PlayingQueueSongAggregation(@Embedded val song: Song, val enqueued: Boolean, val queuePosition: Int) {
    /**
     * Maps the current entry to a [SongDto].
     *
     * @return the song with [queuePosition] attached.
     */
    fun toSongDto(): SongDto = song.toDto(queuePosition)

    /**
     * Builds a playable [MediaItem] for this queue entry.
     *
     * The song's persisted rating is applied as the media item's user rating so
     * the value survives a queue restore.
     *
     * @return a playable, non-browsable [MediaItem] with queue position, song id,
     *   timestamps and play count stored as extras.
     */
    fun toMediaItem(): MediaItem = MediaItem
        .Builder()
        .setMediaId(song.id)
        .setUri(song.filePath.toPlayableUri())
        .setMimeType(song.filePath.inferMimeType())
        .setMediaMetadata(
            MediaMetadata
                .Builder()
                .setTitle(song.title)
                .setTrackNumber(song.trackNumber)
                .setTotalTrackCount(song.totalTracks)
                .setArtist(song.artist)
                .setAlbumTitle(song.album)
                .setReleaseYear(song.albumYear)
                .setArtworkUri(song.albumArt?.toUri())
                .setGenre(song.genre)
                .setDurationMs(song.duration)
                .setUserRating(getStarRating(song.rating))
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .setExtras(getExtras())
                .build(),
        ).build()

    private fun getExtras(): Bundle = Bundle().apply {
        putInt(MediaKeys.QUEUE_POSITION, queuePosition)
        putBoolean(MediaKeys.ENQUEUED, enqueued)
        putString(MediaKeys.SONG_ID, song.id)
        putLong(MediaKeys.CREATED_AT, song.createdAt?.toEpochMilli() ?: 0L)
        putLong(MediaKeys.UPDATED_AT, song.updatedAt?.toEpochMilli() ?: 0L)
        putInt(MediaKeys.PLAY_COUNT, song.playCount)
    }
}
