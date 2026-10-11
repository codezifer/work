package de.carsten.android.muzzic.persistence.migrations

import androidx.sqlite.db.SupportSQLiteDatabase

@Suppress("ClassName")
object V5To6_UpdateSongsEnrichedView : BaseMigration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Centralizes genre normalization and article-aware artist sorting in one view.
        // The statement must stay byte-identical to the generated schema (5.json):
        // Room validates view definitions verbatim, so no IF NOT EXISTS and no
        // reformatting (newlines/indentation are significant).
        db.execSQL("DROP VIEW IF EXISTS `songs_enriched`".trimIndent())
        db.execSQL(
            """
            CREATE VIEW `songs_enriched` AS SELECT s.*,
                    LOWER(REPLACE(REPLACE(s.genre, ' ', ''), '-', '')) AS normalized_genre,
                    CASE
                        WHEN lower(s.artist) LIKE 'the %' THEN substr(s.artist, 5)
                        WHEN lower(s.artist) LIKE 'an %' THEN substr(s.artist, 4)
                        WHEN lower(s.artist) LIKE 'a %' THEN substr(s.artist, 3)
                        ELSE s.artist
                    END AS sort_artist
                FROM songs s
            """.trimIndent(),
        )
    }
}
