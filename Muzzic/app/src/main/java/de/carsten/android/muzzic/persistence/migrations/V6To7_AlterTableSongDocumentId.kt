package de.carsten.android.muzzic.persistence.migrations

import androidx.sqlite.db.SupportSQLiteDatabase
import de.carsten.android.muzzic.UNKNOWN

@Suppress("ClassName")
object V6To7_AlterTableSongDocumentId : BaseMigration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE songs ADD COLUMN documentId TEXT NOT NULL DEFAULT '$UNKNOWN'")
        // Version 7 also changed `songs.rating`'s default from 3 to 128 (WMP rating).
        // SQLite cannot change a column default via ALTER TABLE, so the table is
        // rebuilt to match the schema declared for version 7. This also re-adds the
        // documentId index with the NOCASE collation expected by the entity.
        rebuildSongsTable(db)
    }
}
