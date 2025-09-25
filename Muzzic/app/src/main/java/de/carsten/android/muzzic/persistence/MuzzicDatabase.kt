package de.carsten.android.muzzic.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import de.carsten.android.muzzic.persistence.dao.PlayHistoryDao
import de.carsten.android.muzzic.persistence.dao.PlayingQueueDao
import de.carsten.android.muzzic.persistence.dao.PlaylistDao
import de.carsten.android.muzzic.persistence.dao.SongDao
import de.carsten.android.muzzic.persistence.entity.PlayHistory
import de.carsten.android.muzzic.persistence.entity.PlayingQueue
import de.carsten.android.muzzic.persistence.entity.Playlist
import de.carsten.android.muzzic.persistence.entity.PlaylistSong
import de.carsten.android.muzzic.persistence.entity.Song
import de.carsten.android.muzzic.persistence.migrations.Migrations
import de.carsten.android.muzzic.persistence.utils.Converters

@Database(
    entities = [
        Song::class,
        Playlist::class,
        PlaylistSong::class,
        PlayHistory::class,
        PlayingQueue::class,
    ],
    exportSchema = true,
    version = 1,
    autoMigrations = [],
)
@TypeConverters(Converters::class)
abstract class MuzzicDatabase : RoomDatabase() {
    companion object {
        fun database(context: Context): MuzzicDatabase {
            return Room.databaseBuilder(context, MuzzicDatabase::class.java, "muzzic.db")
                .addMigrations(*Migrations.supply())
                .build()
        }
    }

    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playHistoryDao(): PlayHistoryDao

    abstract fun playingQueueDao(): PlayingQueueDao
}
