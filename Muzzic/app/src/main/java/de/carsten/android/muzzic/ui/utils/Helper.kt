package de.carsten.android.muzzic.ui.utils

import androidx.media3.common.StarRating
import de.carsten.android.muzzic.MAX_STARS
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

/**
 * gets star rating
 *
 * @param rating [Int] wmp9 rating (0..255) integer value
 * @return [StarRating] rating in the range 0f..5f
 */
fun getStarRating(rating: Int? = 128): StarRating = StarRating(
    MAX_STARS,
    when (rating) {
        0 -> 0f
        in 1..25 -> 0.5f
        in 25..51 -> 1.0f
        in 52..75 -> 1.5f
        in 76..102 -> 2.0f
        in 103..128 -> 2.5f
        in 129..153 -> 3.0f
        in 154..178 -> 3.5f
        in 179..204 -> 4.0f
        in 205..225 -> 4.5f
        in 226..255 -> 5.0f
        else -> 2.5f // default rating
    },
)

/**
 * gets wmp rating
 *
 * @param starRating [StarRating] current star / float based rating (0..5f)
 * @return [Int] ]rating in the range of 0..255
 */
fun getWmpRating(starRating: StarRating): Int = when (starRating.starRating) {
    0f -> 0
    in 0f..0.5f -> 25
    in 0.6f..1.0f -> 51
    in 1.1f..1.5f -> 75
    in 1.6f..2.0f -> 102
    in 2.1f..2.5f -> 128
    in 2.6f..3.0f -> 153
    in 3.1f..3.5f -> 178
    in 3.6f..4.0f -> 204
    in 4.1f..4.5f -> 225
    in 4.6f..5.0f -> 255
    else -> 128 // default rating
}
