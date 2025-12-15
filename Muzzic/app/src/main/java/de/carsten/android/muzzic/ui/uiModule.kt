package de.carsten.android.muzzic.ui

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import okio.Path.Companion.toOkioPath
import org.koin.dsl.module

private fun getImageLoader(context: Context): ImageLoader =
    ImageLoader
        .Builder(context)
        .memoryCache {
            MemoryCache
                .Builder()
                .maxSizePercent(context, 0.25)
                .build()
        }.diskCache {
            DiskCache
                .Builder()
                .directory(context.cacheDir.resolve("image_cache").toOkioPath())
                .maxSizePercent(0.02)
                .build()
        }.build()

val uiModule =
    module {
        single { getImageLoader(get()) }
    }
