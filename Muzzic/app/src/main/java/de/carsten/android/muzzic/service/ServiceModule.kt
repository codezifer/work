package de.carsten.android.muzzic.service

import de.carsten.android.muzzic.service.visualizer.VisualizerSink
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val serviceModule =
    module {
        singleOf(::VisualizerSink)
        singleOf(::PlaybackManager)
        factoryOf(::QueueManager)
        factoryOf(::PlaybackAnalytics)
        factoryOf(::PlaybackStateManager)
        singleOf(::AutomaticPlaylistManager)
    }
