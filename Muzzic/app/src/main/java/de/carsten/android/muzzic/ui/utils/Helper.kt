package de.carsten.android.muzzic.ui.utils

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Formats duration (milliseconds) as string format mm:ss"
 *
 * @param durationMs duration milliseconds as [Long]
 * @return formatted duration as [String] with format "mm:ss"
 */
fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

/**
 * Format time as string
 *
 * @param timeMs the time ms as [Long]
 * @return the formatted Datetime as String in format dd.MM.yyyy HH:mm:ss.SSS
 */
fun formatTime(timeMs: Long): String {
    val instant = Instant.ofEpochMilli(timeMs)
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss.SSS").withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}

/**
 * Format time as string
 *
 * @param timeMs the time ms as [String]
 * @return the formatted Datetime as String in format dd.MM.yyyy HH:mm:ss.SSS
 */
fun formatTime(timeMs: String): String = formatTime(timeMs.toLong())

/**
 * Gets artist name without "The", "An" or "A"
 *
 * @return artist name with articles removed
 */
fun artistName(artistName: String): String {
    val lcArtistName = artistName.lowercase()
    return if (lcArtistName.startsWith("the ")) {
        artistName.substring(4)
    } else if (lcArtistName.startsWith("an ")) {
        artistName.substring(3)
    } else if (lcArtistName.startsWith("a ")) {
        artistName.substring(2)
    } else {
        artistName
    }
}
