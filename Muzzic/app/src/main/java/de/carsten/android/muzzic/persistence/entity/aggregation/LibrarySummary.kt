package de.carsten.android.muzzic.persistence.entity.aggregation

/**
 * Summary of the music library contents.
 */
data class LibrarySummary(val artistCount: Int, val albumCount: Int, val songCount: Int, val genreCount: Int, val playlistCount: Int)
