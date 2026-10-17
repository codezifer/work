package de.carsten.android.muzzic.persistence.migrations

import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Recreates the `songs` table with the schema declared by
 * [de.carsten.android.muzzic.persistence.entity.Song].
 *
 * SQLite cannot change a column's `DEFAULT` through `ALTER TABLE`, so every time a
 * default drifts (e.g. `rating` from `3` to `128`) the table has to be rebuilt via
 * the create → copy → drop → rename pattern. The `songs_enriched` view and all
 * indices are dropped together with the old table and recreated afterwards.
 *
 * Room does not enable `PRAGMA foreign_keys` while migrating
 * (`foreign_keys` defaults to `OFF`), so dropping the parent table does not cascade
 * into `playing_queue`; its rows stay untouched because the parent is recreated with
 * the very same `id` values.
 *
 * The DDL below is a frozen snapshot of the entity schema at version 9 and therefore
 * intentionally hardcodes the literal defaults instead of referencing
 * `AppConfig.Persistence.DEFAULT_WMP_RATING` / `UNKNOWN`.
 *
 * @param db the database that is currently being migrated.
 */
internal fun rebuildSongsTable(db: SupportSQLiteDatabase) {
    // The view reads from `songs`; dropping the table invalidates it, so it is
    // dropped up front and recreated once the new table is in place.
    db.execSQL("DROP VIEW IF EXISTS `songs_enriched`")
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS `songs_new` (
            `title` TEXT NOT NULL,
            `trackNumber` INTEGER NOT NULL DEFAULT 0,
            `totalTracks` INTEGER NOT NULL DEFAULT 0,
            `artist` TEXT NOT NULL COLLATE NOCASE,
            `album` TEXT NOT NULL COLLATE NOCASE,
            `genre` TEXT NOT NULL COLLATE NOCASE,
            `duration` INTEGER NOT NULL DEFAULT 0,
            `filePath` TEXT,
            `albumArt` TEXT,
            `albumYear` INTEGER NOT NULL DEFAULT -1,
            `rating` INTEGER NOT NULL DEFAULT 128,
            `playCount` INTEGER NOT NULL DEFAULT 0,
            `lastPlayed` INTEGER NOT NULL DEFAULT 0,
            `documentId` TEXT NOT NULL COLLATE NOCASE DEFAULT 'Unknown',
            `id` TEXT NOT NULL,
            `createdAt` INTEGER DEFAULT CURRENT_TIMESTAMP,
            `updatedAt` INTEGER DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY(`id`)
        )
        """.trimIndent(),
    )
    db.execSQL(
        """
        INSERT INTO `songs_new` (
            `title`,
            `trackNumber`,
            `totalTracks`,
            `artist`,
            `album`,
            `genre`,
            `duration`,
            `filePath`,
            `albumArt`,
            `albumYear`,
            `rating`,
            `playCount`,
            `lastPlayed`,
            `documentId`,
            `id`,
            `createdAt`,
            `updatedAt`
        )
        SELECT
            `title`,
            `trackNumber`,
            `totalTracks`,
            `artist`,
            `album`,
            `genre`,
            `duration`,
            `filePath`,
            `albumArt`,
            `albumYear`,
            `rating`,
            `playCount`,
            `lastPlayed`,
            `documentId`,
            `id`,
            `createdAt`,
            `updatedAt`
        FROM `songs`
        """.trimIndent(),
    )
    db.execSQL("DROP TABLE `songs`")
    db.execSQL("ALTER TABLE `songs_new` RENAME TO `songs`")
    SONGS_INDICES.forEach { db.execSQL(it) }
    db.execSQL(SONGS_ENRICHED_VIEW_SQL)
}

/** Index definitions of the `songs` table, kept in sync with the generated schema. */
internal val SONGS_INDICES: List<String> = listOf(
    "CREATE INDEX IF NOT EXISTS `index_songs_updatedAt` ON `songs` (`updatedAt`)",
    "CREATE INDEX IF NOT EXISTS `index_songs_artist_albumYear_album_trackNumber_title` ON `songs` (`artist`, `albumYear`, `album`, `trackNumber`, `title`)",
    "CREATE INDEX IF NOT EXISTS `index_songs_album_artist` ON `songs` (`album`, `artist`)",
    "CREATE INDEX IF NOT EXISTS `index_songs_genre_rating_playCount` ON `songs` (`genre`, `rating`, `playCount`)",
    "CREATE INDEX IF NOT EXISTS `index_songs_artist` ON `songs` (`artist`)",
    "CREATE INDEX IF NOT EXISTS `index_songs_album` ON `songs` (`album`)",
    "CREATE INDEX IF NOT EXISTS `index_songs_genre` ON `songs` (`genre`)",
    "CREATE INDEX IF NOT EXISTS `index_songs_documentId` ON `songs` (`documentId`)",
)

/**
 * Definition of the `songs_enriched` view.
 *
 * Room validates view definitions verbatim, so the statement must stay byte-identical
 * to the generated schema: no `IF NOT EXISTS` and no reformatting.
 */
internal val SONGS_ENRICHED_VIEW_SQL =
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
    """.trimIndent()
