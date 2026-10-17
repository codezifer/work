package de.carsten.android.muzzic.persistence.migrations

import androidx.sqlite.db.SupportSQLiteDatabase

@Suppress("ClassName")
object V7To8_TrimPlayingQueueMetadata : BaseMigration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Rebuild `playing_queue` so it only references the library song (`songId`)
        // plus its queue state. All duplicated song metadata (title, artist, album,
        // duration, filePath, …) is dropped; the `songs` table is the single source
        // of truth. Rows without a song reference cannot be resolved anymore and are
        // discarded during the copy.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `playing_queue_new` (" +
                "`songId` TEXT NOT NULL, `enqueued` INTEGER NOT NULL, `queuePosition` INTEGER NOT NULL, " +
                "`id` TEXT NOT NULL, `createdAt` INTEGER DEFAULT CURRENT_TIMESTAMP, " +
                "`updatedAt` INTEGER DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`songId`) REFERENCES `songs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL(
            "INSERT OR IGNORE INTO `playing_queue_new` (`songId`, `enqueued`, `queuePosition`, `id`, `createdAt`, `updatedAt`) " +
                "SELECT `songId`, COALESCE(`enqueued`, 0), `queuePosition`, `id`, `createdAt`, `updatedAt` " +
                "FROM `playing_queue` WHERE `songId` IS NOT NULL AND `songId` != ''",
        )
        db.execSQL("DROP TABLE `playing_queue`")
        db.execSQL("ALTER TABLE `playing_queue_new` RENAME TO `playing_queue`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_playing_queue_songId` ON `playing_queue` (`songId`)")
    }
}
