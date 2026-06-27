package de.carsten.android.muzzic.scanning

import org.koin.dsl.module

val scanningModule = module {
    single { MusicFileScanner(get(), get(), get(), get(), get()) }
    single { PlaylistFileScanner(get(), get(), get()) }
}
