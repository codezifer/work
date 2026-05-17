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
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

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

// canonical serialization: length-prefixed UTF-8 bytes to avoid collisions
fun bytesWithLen(s: String): ByteArray {
    val b = s.toByteArray(StandardCharsets.UTF_8)
    val bb = ByteBuffer.allocate(4 + b.size)
    bb.putInt(b.size)
    bb.put(b)
    return bb.array()
}

/**
 * Creates UUID from payload, namespace and SHA-1 hash
 */
fun digestUUID(payload: ByteArray, namespace: UUID): UUID {
    // build name input as namespace bytes + payload
    val nsBytes = ByteBuffer.allocate(16)
        .putLong(namespace.mostSignificantBits)
        .putLong(namespace.leastSignificantBits)
        .array()

    val md = MessageDigest.getInstance("SHA-1")
    md.update(nsBytes)
    md.update(payload)
    val sha1 = md.digest() // 20 bytes

    val uuidBytes = sha1.copyOf(16)
    // set version (5) and variant bits per RFC 4122
    uuidBytes[6] = (uuidBytes[6].toInt() and 0x0f or (5 shl 4)).toByte()
    uuidBytes[8] = (uuidBytes[8].toInt() and 0x3f or 0x80).toByte()

    val bb = ByteBuffer.wrap(uuidBytes)
    val msb = bb.long
    val lsb = bb.long
    return UUID(msb, lsb)
}

/**
 * Converts triple of song title, album and artist to UUID
 *
 * @param title song title as [String]
 * @param album song album as [String]
 * @param artist song artist as [String]
 * @return [UUID]
 */
fun songId(
    title: String,
    album: String,
    artist: String,
    namespace: UUID = UUID.nameUUIDFromBytes(byteArrayOf())
): UUID {
    val payload = bytesWithLen(artist) + bytesWithLen(album) + bytesWithLen(title)
    return digestUUID(payload, namespace)
}

/**
 * Converts single media id to UUID
 *
 * @param name media id arguments as [String]
 * @return [UUID]
 */
fun mediaId(name: String, namespace: UUID = UUID.nameUUIDFromBytes(byteArrayOf())): UUID {
    val payload = bytesWithLen(name)
    return digestUUID(payload, namespace)
}
