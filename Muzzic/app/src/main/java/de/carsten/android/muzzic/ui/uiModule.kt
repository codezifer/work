package de.carsten.android.muzzic.ui

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import org.koin.dsl.module

private fun getImageLoader(context: Context): ImageLoader =
    ImageLoader
        .Builder(context)
        .memoryCache {
            MemoryCache
                .Builder(context)
                .maxSizePercent(0.25)
                .build()
        }.diskCache {
            DiskCache
                .Builder()
                .directory(context.cacheDir.resolve("image_cache"))
                .maxSizePercent(0.02)
                .build()
        }.build()

val uiModule =
    module {
        single { getImageLoader(get()) }
    }
