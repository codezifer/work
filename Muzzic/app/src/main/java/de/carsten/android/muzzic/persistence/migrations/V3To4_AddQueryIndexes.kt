package de.carsten.android.muzzic.persistence.migrations

import androidx.sqlite.db.SupportSQLiteDatabase

@Suppress("ClassName")
object V3To4_AddQueryIndexes : BaseMigration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Speeds up the time-based statistics queries (monthly/genre/top songs).
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_play_history_playedAt` ON `play_history` (`playedAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_updatedAt` ON `songs` (`updatedAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_artist_albumYear_album_trackNumber_title` ON `songs` (`artist`, `albumYear`, `album`, `trackNumber`, `title`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_album_artist` ON `songs` (`album`, `artist`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_genre_rating_playCount` ON `songs` (`genre`, `rating`, `playCount`)")
    }
}
