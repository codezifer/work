package de.carsten.android.muzzic.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil3.request.ImageRequest
import coil3.request.crossfade

/**
 * Remembers a stable Coil [ImageRequest] for album covers.
 *
 * Uses the full cover path as explicit memory/disk/placeholder-memory cache key, so scrolling
 * a lazy list back to an item hits the memory cache synchronously instead of flashing the
 * placeholder while [de.carsten.android.muzzic.ui.AlbumArtFetcher] re-reads the file. The fixed
 * [sizePx] keeps the cache key independent of the measured layout size.
 *
 * @param data the image data (cover path string, Uri, stream, drawable res, or null).
 * @param cacheKey stable cache key, e.g. the full cover path string; null skips explicit keys.
 * @param sizePx requested square edge length in pixels.
 * @return the remembered [ImageRequest].
 */
@Composable
fun rememberCoverImageRequest(data: Any?, cacheKey: String?, sizePx: Int): ImageRequest {
    val context = LocalContext.current
    return remember(data, cacheKey, sizePx) {
        ImageRequest
            .Builder(context)
            .data(data)
            .memoryCacheKey(cacheKey)
            .diskCacheKey(cacheKey)
            .placeholderMemoryCacheKey(cacheKey)
            .size(sizePx)
            .crossfade(true)
            .build()
    }
}
