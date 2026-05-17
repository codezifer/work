package de.carsten.android.muzzic.ui

import android.annotation.SuppressLint
import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.map.Mapper
import coil3.memory.MemoryCache
import coil3.request.Options
import de.carsten.android.muzzic.model.AlbumArtUri
import de.carsten.android.muzzic.service.MediaLibraryManager
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

private fun getImageLoader(context: Context): ImageLoader {
    val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    return ImageLoader
        .Builder(context)
        .components {
            add(object : Mapper<String, AlbumArtUri> {
                override fun map(data: String, options: Options): AlbumArtUri? {
                    if (data.startsWith("file://") && data.contains("offset=")) {
                        return AlbumArtUri.parse(data)
                    }
                    return null
                }
            })
            add(AlbumArtFetcher.Factory(context, okHttpClient))
        }
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
}

@SuppressLint("UnsafeOptInUsageError")
val uiModule = module {
    single { getImageLoader(get()) }
    single { MediaLibraryManager(get()) }
}
