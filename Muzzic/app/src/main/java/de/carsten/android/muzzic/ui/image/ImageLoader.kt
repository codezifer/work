package de.carsten.android.muzzic.ui.image

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.util.DebugLogger
import de.carsten.android.muzzic.BuildConfig
import de.carsten.android.muzzic.IMAGE_CACHE
import de.carsten.android.muzzic.ui.AlbumArtFetcher
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath
import java.util.concurrent.TimeUnit

fun ImageLoader(context: Context): ImageLoader {
    val okHttpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    return ImageLoader
        .Builder(context)
        .logger(if (BuildConfig.DEBUG) DebugLogger() else null)
        .apply {
            // Startup timing markers are measurement-only and stay out of release builds.
            if (BuildConfig.DEBUG) eventListenerFactory { StartupImageListener(it) }
        }
        // No crossfade: Coil's 200 ms fade only delays cold covers (issues.md 0020).
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
