package de.carsten.android.muzzic.service

import android.annotation.SuppressLint
import androidx.annotation.OptIn
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

@SuppressLint("UnsafeOptInUsageError")
@OptIn(UnstableApi::class)
val serviceModule =
    module {
        singleOf(::VisualizerSink)
        factoryOf(::PlaybackManager)
        factoryOf(::QueueManager)
        factoryOf(::PlaybackAnalytics)
        factoryOf(::PlaybackStateManager)
        singleOf(::AutomaticPlaylistManager)
        singleOf(::CoilBitmapLoader) bind BitmapLoader::class
    }
