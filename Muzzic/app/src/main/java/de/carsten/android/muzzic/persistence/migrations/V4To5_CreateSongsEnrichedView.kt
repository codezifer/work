package de.carsten.android.muzzic.persistence.migrations

import androidx.sqlite.db.SupportSQLiteDatabase

@Suppress("ClassName")
object V4To5_CreateSongsEnrichedView : BaseMigration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Centralizes genre normalization and article-aware artist sorting in one view.
        // The statement must stay byte-identical to the generated schema (5.json):
        // Room validates view definitions verbatim. See [SONGS_ENRICHED_VIEW_SQL].
        db.execSQL(SONGS_ENRICHED_VIEW_SQL)
    }
}
