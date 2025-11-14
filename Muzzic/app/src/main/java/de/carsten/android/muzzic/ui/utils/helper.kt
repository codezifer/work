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
