package de.carsten.android.muzzic.persistence.migrations

import androidx.sqlite.db.SupportSQLiteDatabase

@Suppress("ClassName")
object V2To3_RestorePlayHistory : BaseMigration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `play_history` (`songId` TEXT NOT NULL, `playedAt` INTEGER NOT NULL, " +
                "`id` TEXT NOT NULL, `createdAt` INTEGER DEFAULT CURRENT_TIMESTAMP, " +
                "`updatedAt` INTEGER DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`songId`) REFERENCES `songs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_play_history_songId` ON `play_history` (`songId`)")
    }
}
