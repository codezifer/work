package de.carsten.android.muzzic.ui

import android.annotation.SuppressLint
import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import coil3.util.DebugLogger
import de.carsten.android.muzzic.AppConfig
import de.carsten.android.muzzic.IMAGE_CACHE
import de.carsten.android.muzzic.service.MediaLibraryManager
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
        .logger(DebugLogger())
        .crossfade(AppConfig.Ui.DEFAULT_CROSSFADE)
        .components {
            add(AlbumArtFetcher.Mapper())
            add(AlbumArtFetcher.Factory(context))
            add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient }))
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
