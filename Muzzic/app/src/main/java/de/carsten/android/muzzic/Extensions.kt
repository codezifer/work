package de.carsten.android.muzzic

import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

private const val TAG = "EXTENSIONS"

/**
 * Playback state to string
 *
 * @param state player state as [Int]
 * @return state name as [String]
 */
fun playbackStateToString(state: Int): String = when (state) {
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
fun MediaItem.mediaItemInstant(key: String): Instant = this.mediaMetadata.extras?.getLong(key).let { time ->
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
    val nsBytes =
        ByteBuffer
            .allocate(16)
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
fun songId(title: String, album: String, artist: String, namespace: UUID = UUID.nameUUIDFromBytes(byteArrayOf())): UUID {
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

/**
 * Converts a file path to a playable Uri, ensuring local files use the file:// scheme
 * and are properly encoded.
 */
fun String?.toPlayableUri(): Uri? {
    if (this == null) return null
    return if (this.startsWith("/") || this.startsWith("file://")) {
        val actualPath = if (this.startsWith("file://")) this.substring(7) else this
        Uri.fromFile(File(actualPath))
    } else {
        this.toUri()
    }
}

/**
 * Infers the MIME type from a file path or URI string.
 */
fun String?.inferMimeType(): String? {
    if (this == null) return null
    val extension = this.substringAfterLast('.', "").lowercase()
    return when (extension) {
        "mp3" -> "audio/mpeg"
        "ogg" -> "audio/ogg"
        "flac" -> "audio/flac"
        "mp4", "m4a" -> "audio/mp4"
        else -> null
    }
}
