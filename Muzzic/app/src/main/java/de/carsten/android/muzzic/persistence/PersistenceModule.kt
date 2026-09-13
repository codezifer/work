package de.carsten.android.muzzic.persistence

import de.carsten.android.muzzic.persistence.repo.AlbumRepository
import de.carsten.android.muzzic.persistence.repo.AppSettingsRepository
import de.carsten.android.muzzic.persistence.repo.ArtistRepository
import de.carsten.android.muzzic.persistence.repo.GenreRepository
import de.carsten.android.muzzic.persistence.repo.MusicRepository
import de.carsten.android.muzzic.persistence.repo.PlayingQueueRepository
import de.carsten.android.muzzic.persistence.repo.PlaylistRepository
import de.carsten.android.muzzic.persistence.repo.SettingsRepository
import de.carsten.android.muzzic.persistence.repo.SongRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule =
    module {
        single { MuzzicDatabase.database(androidContext()) }
        single { get<MuzzicDatabase>().songDao() }
        single { get<MuzzicDatabase>().artistDao() }
        single { get<MuzzicDatabase>().albumDao() }
        single { get<MuzzicDatabase>().genreDao() }
        single { get<MuzzicDatabase>().playlistDao() }
        single { get<MuzzicDatabase>().playHistoryDao() }
        single { get<MuzzicDatabase>().playingQueueDao() }
        single { get<MuzzicDatabase>().playerSettingsDao() }
        single { get<MuzzicDatabase>().genericSettingDao() }
    }

val repoModule =
    module {
        single {
            MusicRepository(
                songDao = get(),
                playHistoryDao = get(),
                playlistDao = get(),
                playingQueueDao = get(),
                database = get(),
                context = androidContext(),
                musicFileScanner = get(),
                playlistFileScanner = get(),
                genreRepository = get(),
            )
        }
        single { ArtistRepository(get(), get()) }
        single { AlbumRepository(get()) }
        single { GenreRepository(get()) }
        single { SongRepository(get()) }
        single { PlaylistRepository(get()) }
        single { PlayingQueueRepository(get()) }
        single { SettingsRepository(get()) }
        single { AppSettingsRepository(get()) }
    }
