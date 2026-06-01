package de.carsten.android.muzzic.ui.utils

const val EMPTY = "<EMPTY>"

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
 * Parsed raw ID3 year as string which could have a format like "2025" or "2025-01-02"
 *
 * @param year raw ID3 year (TYER) as [String]
 * @return parsed year as [Int] (-1 if source string is null or empty)
 */
fun parseId3Year(year: String?): Int {
    if (year.isNullOrBlank()) return -1

    val regex = Regex("^(\\d{4})")
    val match = regex.find(year)
    return match
        ?.groups
        ?.get(1)
        ?.value
        ?.toIntOrNull() ?: -1
}
