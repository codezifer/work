package de.carsten.android.muzzic.viewmodel

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule =
    module {
        viewModelOf(::PlayerViewModel)
        viewModelOf(::LibraryViewModel)
        viewModelOf(::StatisticsViewModel)
        viewModelOf(::PlayingQueueViewModel)
    }
