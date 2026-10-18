package de.carsten.android.muzzic.persistence.migrations

import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Object holding view for recreation
 */
object Views {
    const val SONGS_ENRICHED_DROP = "DROP VIEW IF EXISTS songs_enriched"
    val SONGS_ENRICHED_CREATE =
        """
        CREATE VIEW IF NOT EXISTS songs_enriched AS
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
        """.trimIndent()

    /**
     * Recreate songs_enriched view after songs table changed
     *
     * @param db room database
     */
    fun recreateSongsEnriched(db: SupportSQLiteDatabase) {
        db.execSQL(SONGS_ENRICHED_DROP)
        db.execSQL(SONGS_ENRICHED_CREATE)
    }
}
