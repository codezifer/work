package de.carsten.android.muzzic.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import de.carsten.android.muzzic.AppConfig
import de.carsten.android.muzzic.persistence.dao.AlbumDao
import de.carsten.android.muzzic.persistence.dao.ArtistDao
import de.carsten.android.muzzic.persistence.dao.GenericSettingDao
import de.carsten.android.muzzic.persistence.dao.GenreDao
import de.carsten.android.muzzic.persistence.dao.PlayHistoryDao
import de.carsten.android.muzzic.persistence.dao.PlayerSettingsDao
import de.carsten.android.muzzic.persistence.dao.PlayingQueueDao
import de.carsten.android.muzzic.persistence.dao.PlaylistDao
import de.carsten.android.muzzic.persistence.dao.SongDao
import de.carsten.android.muzzic.persistence.entity.GenericSetting
import de.carsten.android.muzzic.persistence.entity.PlayHistory
import de.carsten.android.muzzic.persistence.entity.PlayerSettings
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
        PlayerSettings::class,
        GenericSetting::class,
    ],
    exportSchema = true,
    version = 1,
    autoMigrations = [],
)
@TypeConverters(Converters::class)
abstract class MuzzicDatabase : RoomDatabase() {
    companion object {
        fun database(context: Context): MuzzicDatabase = Room
            .databaseBuilder(context, MuzzicDatabase::class.java, AppConfig.Persistence.DATABASE_NAME)
            .addMigrations(*Migrations.supply())
            .fallbackToDestructiveMigration(dropAllTables = false)
            .build()
    }

    abstract fun songDao(): SongDao

    abstract fun artistDao(): ArtistDao

    abstract fun albumDao(): AlbumDao

    abstract fun genreDao(): GenreDao

    abstract fun playlistDao(): PlaylistDao

    abstract fun playHistoryDao(): PlayHistoryDao

    abstract fun playingQueueDao(): PlayingQueueDao

    abstract fun playerSettingsDao(): PlayerSettingsDao

    abstract fun genericSettingDao(): GenericSettingDao
}
