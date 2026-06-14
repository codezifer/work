package de.carsten.android.muzzic.ui

import android.annotation.SuppressLint
import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import de.carsten.android.muzzic.service.MediaLibraryManager
import de.carsten.android.muzzic.utils.IMAGE_CACHE
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath
import org.koin.dsl.module

private fun getImageLoader(context: Context): ImageLoader {
    val okHttpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    return ImageLoader
        .Builder(context)
        .components {
            add(AlbumArtFetcher.Mapper())
            add(AlbumArtFetcher.Factory(context, okHttpClient))
        }.memoryCache {
            MemoryCache
                .Builder()
                .maxSizePercent(context, 0.25)
                .build()
        }.diskCache {
            DiskCache
                .Builder()
                .directory(context.cacheDir.resolve(IMAGE_CACHE).toOkioPath())
                .maxSizePercent(0.02)
                .build()
        }.build()
}

@SuppressLint("UnsafeOptInUsageError")
val uiModule =
    module {
        single { getImageLoader(get()) }
        single { MediaLibraryManager(get()) }
    }
