package de.carsten.android.muzzic.persistence

import de.carsten.android.muzzic.persistence.repo.AlbumRepository
import de.carsten.android.muzzic.persistence.repo.ArtistRepository
import de.carsten.android.muzzic.persistence.repo.MusicRepository
import de.carsten.android.muzzic.persistence.repo.SongRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single { MuzzicDatabase.database(androidContext()) }
    single { get<MuzzicDatabase>().songDao() }
    single { get<MuzzicDatabase>().playlistDao() }
    single { get<MuzzicDatabase>().playHistoryDao() }
}

val repoModule = module {
    single {
        MusicRepository(
            songDao = get(),
            playlistDao = get(),
            playHistoryDao = get(),
            context = androidContext()
        )
    }
    single { ArtistRepository(get()) }
    single { AlbumRepository(get()) }
    single { SongRepository(get()) }
}
