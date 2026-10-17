package de.carsten.android.muzzic.persistence.migrations

import androidx.sqlite.db.SupportSQLiteDatabase

@Suppress("ClassName")
object V8To9_FixSongsRatingDefault : BaseMigration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Installations that upgraded from version <= 6 kept `songs.rating ... DEFAULT 3`
        // because the default was changed to 128 without rebuilding the table. Room
        // validates the schema after migrating and aborted the upgrade, so the table is
        // rebuilt here to repair every already-deployed database.
        rebuildSongsTable(db)
    }
}
