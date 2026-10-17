package de.carsten.android.muzzic.ui.utils

import androidx.media3.common.StarRating
import de.carsten.android.muzzic.MAX_STARS
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.abs
import kotlin.math.roundToInt

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
 * Formats a date as a localized short date string (without time).
 *
 * @param instant the date to format.
 * @return the formatted date, e.g. "12.03.2026" for German locale.
 */
fun formatDate(instant: Instant): String {
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}

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
 * WMP9 rating anchor values for the eleven 0.5-star steps from 0 to 5 stars.
 * These are the canonical Windows Media Player values (0 = unrated, 255 = 5 stars);
 * every value in between is snapped to the nearest anchor.
 */
private val WMP_RATING_ANCHORS = intArrayOf(0, 25, 51, 75, 102, 128, 153, 178, 204, 225, 255)

/**
 * Number of half-star steps spanned by the rating (5 stars * 2).
 */
private const val RATING_HALF_STEPS = 10

/**
 * Converts a WMP9 rating (0..255) into a star value in the range 0f..5f,
 * quantized to 0.5 steps.
 *
 * @param rating WMP9 rating (0..255) or `null` when unrated.
 * @return star value in the range 0f..5f, e.g. 2.5f.
 */
fun getStars(rating: Int?): Float {
    val wmpValue = rating ?: 0
    val nearestIndex = WMP_RATING_ANCHORS.indices.minByOrNull { index ->
        abs(WMP_RATING_ANCHORS[index] - wmpValue)
    } ?: 0
    return nearestIndex / 2f
}

/**
 * Gets the Media3 star rating for a WMP9 value.
 *
 * @param rating WMP9 rating (0..255) or `null` when unrated.
 * @return [StarRating] in the range 0f..5f.
 */
fun getStarRating(rating: Int? = 0): StarRating = StarRating(MAX_STARS, getStars(rating))

/**
 * Converts a star value into the canonical WMP9 rating.
 *
 * @param stars star value in the range 0f..5f (0.5 steps).
 * @return WMP9 rating in the range 0..255.
 */
fun getWmpRating(stars: Float): Int {
    val stepIndex = (stars * 2f).roundToInt().coerceIn(0, RATING_HALF_STEPS)
    return WMP_RATING_ANCHORS[stepIndex]
}

/**
 * Gets the WMP9 rating for a Media3 star rating.
 *
 * @param starRating current star / float based rating (0..5f).
 * @return WMP9 rating in the range 0..255.
 */
fun getWmpRating(starRating: StarRating): Int = getWmpRating(starRating.starRating)
