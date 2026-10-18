package de.carsten.android.muzzic.persistence.view

import androidx.room.ColumnInfo
import androidx.room.DatabaseView
import androidx.room.Embedded
import de.carsten.android.muzzic.persistence.entity.Song

/**
 * Songs enriched with derived grouping and sorting keys.
 *
 * Centralizes the two normalizations that were previously copy-pasted across DAO
 * queries: the genre key mirrors [de.carsten.android.muzzic.utils.GenreUtils.normalizeKey]
 * (lowercase, spaces and hyphens removed), and [sortArtist] strips a leading article
 * ("The ", "An ", "A ") for article-aware ordering.
 *
 * Database identifiers are snake_case by convention ([normalized_genre], [sort_artist]);
 * the Kotlin properties stay camelCase.
 */
@DatabaseView(
    viewName = SongsEnriched.VIEW_NAME,
    value = """
    SELECT s.*,
        LOWER(REPLACE(REPLACE(s.genre, ' ', ''), '-', '')) AS normalized_genre,
        CASE
            WHEN lower(s.artist) LIKE 'the %' THEN substr(s.artist, 5)
            WHEN lower(s.artist) LIKE 'an %' THEN substr(s.artist, 4)
            WHEN lower(s.artist) LIKE 'a %' THEN substr(s.artist, 3)
            ELSE s.artist
        END AS sort_artist,
        strftime('%Y-%m-%d %H:%M:%S', s.lastPlayed / 1000, 'unixepoch') AS lastPlayed_h,
        strftime('%Y-%m-%d %H:%M:%S', s.createdAt / 1000, 'unixepoch') AS createdAt_h,
        strftime('%Y-%m-%d %H:%M:%S', s.updatedAt / 1000, 'unixepoch') AS updatedAt_h
    FROM songs s
    """,
)
data class SongsEnriched(@Embedded val song: Song, @ColumnInfo(name = "normalized_genre") val normalizedGenre: String, @ColumnInfo(name = "sort_artist") val sortArtist: String) {
    companion object {
        const val VIEW_NAME = "songs_enriched"
    }
}
