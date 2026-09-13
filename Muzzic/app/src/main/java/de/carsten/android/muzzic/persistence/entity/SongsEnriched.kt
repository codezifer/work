package de.carsten.android.muzzic.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.DatabaseView
import androidx.room.Embedded

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
    viewName = "songs_enriched",
    value = """
    SELECT s.*,
        LOWER(REPLACE(REPLACE(s.genre, ' ', ''), '-', '')) AS normalized_genre,
        CASE WHEN s.artist LIKE 'The %' THEN substr(s.artist, 5) WHEN s.artist LIKE 'An %' THEN substr(s.artist, 4) WHEN s.artist LIKE 'A %' THEN substr(s.artist, 3) ELSE s.artist END AS sort_artist
    FROM songs s
    """,
)
data class SongsEnriched(@Embedded val song: Song, @ColumnInfo(name = "normalized_genre") val normalizedGenre: String, @ColumnInfo(name = "sort_artist") val sortArtist: String)
