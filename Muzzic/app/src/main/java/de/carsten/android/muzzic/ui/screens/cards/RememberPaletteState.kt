package de.carsten.android.muzzic.ui.screens.cards

import android.annotation.SuppressLint
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil3.SingletonImageLoader
import coil3.asDrawable
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import de.carsten.android.muzzic.logging.rememberLogger
import de.carsten.android.muzzic.model.AlbumArtUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val PALETTE_CACHE_SIZE = 200
private const val PALETTE_MAX_COLOR_COUNT = 24

private val paletteCache = LruCache<String, Palette>(PALETTE_CACHE_SIZE)

/**
 * Returns a stable cache key for the palette cache.
 *
 * Uses the full [AlbumArtUri.get] string (including file path) instead of
 * [AlbumArtUri.equals], which ignores the file path and would collide for
 * different files with identical size/hash metadata.
 *
 * @param source the palette image source.
 * @return a stable cache key, or null if [source] is null.
 */
private fun paletteCacheKey(source: Any?): String? = when (source) {
    null -> null
    is AlbumArtUri -> source.get()
    else -> source.toString()
}

/**
 * Remembers the [Palette] for the given image [source].
 *
 * Loads the image with hardware bitmaps disabled (required for pixel access),
 * generates the palette off the calling thread and caches the result in a
 * shared LRU cache keyed by [paletteCacheKey].
 *
 * @param source the image data (cover path string, Uri, [AlbumArtUri], or null).
 * @return the palette state, null while loading or on failure.
 */
@SuppressLint("LocalContextResourcesRead")
@Composable
fun rememberPaletteState(source: Any?): State<Palette?> {
    val context = LocalContext.current
    val cacheKey = remember(source) { paletteCacheKey(source) }
    val initialPalette = remember(cacheKey) { cacheKey?.let { paletteCache.get(it) } }
    val paletteState = remember(cacheKey) { mutableStateOf<Palette?>(initialPalette) }
    val logger = rememberLogger("RememberPaletteState")

    LaunchedEffect(cacheKey) {
        val key = cacheKey ?: return@LaunchedEffect
        if (paletteState.value != null) {
            return@LaunchedEffect
        }

        withContext(Dispatchers.IO) {
            val loader = SingletonImageLoader.get(context)
            val request =
                ImageRequest
                    .Builder(context)
                    .data(source)
                    .allowHardware(false) // Required for Palette to read pixels
                    .build()

            when (val result = loader.execute(request)) {
                is SuccessResult -> {
                    val bitmap = result.image.asDrawable(context.resources).toBitmap()
                    val newPalette = Palette
                        .from(bitmap)
                        .maximumColorCount(PALETTE_MAX_COLOR_COUNT)
                        .generate()

                    paletteCache.put(key, newPalette)
                    paletteState.value = newPalette
                }

                is ErrorResult -> {
                    logger.error("Failed to load image for palette: $source", result.throwable)
                }
            }
        }
    }
    return paletteState
}
