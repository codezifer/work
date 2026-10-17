package de.carsten.android.muzzic.persistence.entity.aggregation

import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.room.Embedded
import de.carsten.android.muzzic.AppConfig
import de.carsten.android.muzzic.EMPTY
import de.carsten.android.muzzic.UNKNOWN
import de.carsten.android.muzzic.inferMimeType
import de.carsten.android.muzzic.model.MediaKeys
import de.carsten.android.muzzic.persistence.entity.PlayingQueue
import de.carsten.android.muzzic.toPlayableUri
import de.carsten.android.muzzic.ui.model.SongDto
import de.carsten.android.muzzic.ui.utils.getStarRating

/**
 * Read model combining a [PlayingQueue] entry with the matching song's values from
 * the `songs` table.
 *
 * Built by joining `playing_queue` with `songs` on the queue's song id so a queue
 * restored from the database still exposes the persisted rating. The join is a
 * `LEFT JOIN`, so a queue entry whose song was deleted still yields a row; its
 * [rating] and [playCount] then fall back to their defaults.
 *
 * @property playingQueue the persisted queue entry (identity and playback metadata).
 * @property playCount number of times the song has been played, taken from `songs`.
 * @property rating the song rating on the Windows Media Player / ID3 POPM scale
 *   `0..255`; converted to a 0..5 star value via [getStarRating].
 */
data class PlayingQueueSongAggregation(@Embedded val playingQueue: PlayingQueue, val playCount: Int = 0, val rating: Int = AppConfig.Persistence.DEFAULT_WMP_RATING) {

    fun toSongDto() = SongDto(
        title = playingQueue.title ?: UNKNOWN,
        trackNumber = playingQueue.trackNumber ?: -1,
        totalTracks = playingQueue.totalTracks ?: -1,
        artist = playingQueue.artist ?: UNKNOWN,
        album = playingQueue.album ?: UNKNOWN,
        albumYear = playingQueue.albumYear ?: -1,
        albumArt = playingQueue.albumArt ?: UNKNOWN,
        genre = playingQueue.genre ?: UNKNOWN,
        duration = playingQueue.duration ?: -1L,
        filePath = playingQueue.filePath ?: EMPTY,
        queuePosition = playingQueue.queuePosition,
        id = playingQueue.songId ?: EMPTY,
    )


    /**
     * Builds a playable [MediaItem] for this queue entry.
     *
     * The persisted [rating] is applied as the media item's user rating so the
     * value survives a queue restore.
     *
     * @return a playable, non-browsable [MediaItem] with queue position, song id,
     *   timestamps and [playCount] stored as extras.
     */
    fun toMediaItem(): MediaItem = MediaItem
        .Builder()
        .setMediaId(playingQueue.id)
        .setUri(playingQueue.filePath.toPlayableUri())
        .setMimeType(playingQueue.filePath.inferMimeType())
        .setMediaMetadata(
            MediaMetadata
                .Builder()
                .setTitle(playingQueue.title)
                .setTrackNumber(playingQueue.trackNumber)
                .setTotalTrackCount(playingQueue.totalTracks)
                .setArtist(playingQueue.artist)
                .setAlbumTitle(playingQueue.album)
                .setReleaseYear(playingQueue.albumYear)
                .setArtworkUri(playingQueue.albumArt?.toUri())
                .setGenre(playingQueue.genre)
                .setDurationMs(playingQueue.duration)
                .setUserRating(getStarRating(rating))
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .setExtras(getExtras())
                .build(),
        ).build()

    private fun getExtras(): Bundle = Bundle().apply {
        putInt(MediaKeys.QUEUE_POSITION, playingQueue.queuePosition)
        putBoolean(MediaKeys.ENQUEUED, playingQueue.enqueued ?: false)
        putString(MediaKeys.SONG_ID, playingQueue.songId ?: "")
        putLong(MediaKeys.CREATED_AT, playingQueue.createdAt?.toEpochMilli() ?: 0L)
        putLong(MediaKeys.UPDATED_AT, playingQueue.updatedAt?.toEpochMilli() ?: 0L)
        putInt(MediaKeys.PLAY_COUNT, playCount)
    }
}
