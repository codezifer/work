package de.carsten.android.muzzic.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.Log
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import java.io.IOException
import java.time.Instant

private const val TAG = "EXTENSIONS"

/**
 * extracts album art from music file
 *
 * @param context android context
 * @param audioFilePath absolute audio file path
 * @return opt. Bitmap
 */
fun extractAlbumArt(
    context: Context,
    audioFilePath: String?,
): Bitmap? {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(context, audioFilePath?.toUri())
        val albumArtBytes = retriever.embeddedPicture
        if (albumArtBytes != null) {
            return BitmapFactory.decodeByteArray(albumArtBytes, 0, albumArtBytes.size)
        }
    } catch (e: IllegalArgumentException) {
        Log.e(TAG, e.message ?: "An unknown illegal argument exception occurred!")
    } catch (e: RuntimeException) {
        Log.e(TAG, e.message ?: "An unknown runtime exception occurred!")
    } catch (e: IOException) {
        Log.e(TAG, e.message ?: "An unknown IO exception occurred!")
    } finally {
        try {
            retriever.release()
        } catch (e: IOException) {
            Log.e(
                TAG,
                e.message ?: "An unknown IO exception occurred while releasing media meta data!",
            )
        }
    }
    return null
}

/**
 * Playback state to string
 *
 * @param state player state as [Int]
 * @return state name as [String]
 */
fun playbackStateToString(state: Int): String =
    when (state) {
        Player.STATE_IDLE -> "IDLE"
        Player.STATE_BUFFERING -> "BUFFERING"
        Player.STATE_READY -> "READY"
        Player.STATE_ENDED -> "ENDED"
        else -> "UNKNOWN$state"
    }

/**
 * Gets datetime from media item metadata extras
 *
 * @param key key of the datetime
 * @return [Instant]
 */
fun MediaItem.mediaItemInstant(key: String): Instant =
    this.mediaMetadata.extras?.getLong(key).let { time ->
        if (time == null) {
            Instant.now()
        } else {
            Instant.ofEpochMilli(time)
        }
    }
