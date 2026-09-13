package de.carsten.android.muzzic.playlist

/**
 * Data class representing an entry within an M3U playlist file.
 *
 * @property path The absolute or relative path to the music file.
 * @property title The title of the song extracted from `#EXTINF`, if available.
 * @property duration The duration of the song in seconds from `#EXTINF`, if available.
 */
data class M3uEntry(val path: String, val title: String? = null, val duration: Int? = null)
