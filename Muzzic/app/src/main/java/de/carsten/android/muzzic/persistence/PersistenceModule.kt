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
import de.carsten.android.muzzic.scanning.FileScanner
import de.carsten.android.muzzic.scanning.MusicFileScanner
import de.carsten.android.muzzic.scanning.PlaylistFileScanner
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
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
        single<FileScanner>(named("MusicScanner")) { MusicFileScanner(get(), get(), get(), get(), get()) }
        single<FileScanner>(named("PlaylistScanner")) { PlaylistFileScanner(get(), get(), get()) }
        // Keep concrete versions for injection into MusicRepository if needed,
        // or just use qualifiers there too.
        single { get<FileScanner>(named("MusicScanner")) as MusicFileScanner }
        single { get<FileScanner>(named("PlaylistScanner")) as PlaylistFileScanner }

        single {
            MusicRepository(
                songDao = get(),
                playHistoryDao = get(),
                context = androidContext(),
                musicFileScanner = get(),
                playlistFileScanner = get(),
            )
        }
        single { ArtistRepository(get()) }
        single { AlbumRepository(get()) }
        single { GenreRepository(get()) }
        single { SongRepository(get()) }
        single { PlaylistRepository(get()) }
        single { PlayingQueueRepository(get()) }
        single { SettingsRepository(get()) }
        single { AppSettingsRepository(get()) }
    }
