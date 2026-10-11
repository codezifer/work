package de.carsten.android.muzzic.persistence.migrations

import androidx.sqlite.db.SupportSQLiteDatabase
import de.carsten.android.muzzic.UNKNOWN

@Suppress("ClassName")
object V6To7_AlterTableSongDocumentId : BaseMigration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE songs ADD COLUMN documentId TEXT NOT NULL DEFAULT '$UNKNOWN'")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_documentId` ON `songs` (`documentId`)")
    }
}
