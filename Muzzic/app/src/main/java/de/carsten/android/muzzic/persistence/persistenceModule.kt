package de.carsten.android.muzzic.persistence

import androidx.room.Room
import de.carsten.android.muzzic.persistence.repo.MusicRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            MuzzicDatabase::class.java,
            "muzzic_database"
        ).build()
    }

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
}
