package de.carsten.android.muzzic.persistence.migrations

import androidx.sqlite.db.SupportSQLiteDatabase

@Suppress("ClassName")
object V5To6_UpdateSongsEnrichedView : BaseMigration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Replaces the case-sensitive sort_artist logic with the case-insensitive
        // variant used from version 6 onwards. The statement must stay byte-identical
        // to the generated schema (6.json): Room validates view definitions verbatim.
        // See [SONGS_ENRICHED_VIEW_SQL].
        db.execSQL("DROP VIEW IF EXISTS `songs_enriched`")
        db.execSQL(SONGS_ENRICHED_VIEW_SQL)
    }
}
