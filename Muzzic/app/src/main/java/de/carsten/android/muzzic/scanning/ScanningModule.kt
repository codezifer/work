package de.carsten.android.muzzic.scanning

import org.koin.core.qualifier.named
import org.koin.dsl.module

val scanningModule = module {
    single<FileScanner>(named(MusicFileScanner.SCANNER_ID)) { MusicFileScanner(get(), get(), get(), get(), get()) }
    single<FileScanner>(named(PlaylistFileScanner.SCANNER_ID)) { PlaylistFileScanner(get(), get(), get(), get()) }
    single { get<FileScanner>(named(MusicFileScanner.SCANNER_ID)) as MusicFileScanner }
    single { get<FileScanner>(named(PlaylistFileScanner.SCANNER_ID)) as PlaylistFileScanner }
}
