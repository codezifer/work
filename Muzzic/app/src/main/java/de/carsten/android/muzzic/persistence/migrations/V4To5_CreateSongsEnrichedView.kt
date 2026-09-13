package de.carsten.android.muzzic.persistence.migrations

import androidx.sqlite.db.SupportSQLiteDatabase

@Suppress("ClassName")
object V4To5_CreateSongsEnrichedView : BaseMigration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Centralizes genre normalization and article-aware artist sorting in one view.
        // The statement must stay byte-identical to the generated schema (5.json):
        // Room validates view definitions verbatim, so no IF NOT EXISTS and no
        // reformatting (newlines/indentation are significant).
        db.execSQL(
            """
            CREATE VIEW `songs_enriched` AS SELECT s.*,
                    LOWER(REPLACE(REPLACE(s.genre, ' ', ''), '-', '')) AS normalized_genre,
                    CASE WHEN s.artist LIKE 'The %' THEN substr(s.artist, 5) WHEN s.artist LIKE 'An %' THEN substr(s.artist, 4) WHEN s.artist LIKE 'A %' THEN substr(s.artist, 3) ELSE s.artist END AS sort_artist
                FROM songs s
            """.trimIndent(),
        )
    }
}
