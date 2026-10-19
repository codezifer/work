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
import coil3.request.SuccessResult
import de.carsten.android.muzzic.logging.rememberLogger
import de.carsten.android.muzzic.ui.component.coverBaseKey
import de.carsten.android.muzzic.ui.component.rememberPaletteImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val PALETTE_CACHE_SIZE = 200
private const val PALETTE_MAX_COLOR_COUNT = 24

private val paletteCache = LruCache<String, Palette>(PALETTE_CACHE_SIZE)

/**
 * Remembers the [Palette] for the given image [source].
 *
 * Loads the image through [rememberPaletteImageRequest], which requests a small software bitmap:
 * hardware bitmaps cannot be read by [Palette], and the display resolution is not needed for
 * colour extraction. The resulting palette is cached in a shared LRU keyed by [coverBaseKey].
 *
 * @param source the image data (cover path string, Uri, AlbumArtUri, or null).
 * @return the palette state, null while loading or on failure.
 */
@SuppressLint("LocalContextResourcesRead")
@Composable
fun rememberPaletteState(source: Any?): State<Palette?> {
    val context = LocalContext.current
    val cacheKey = remember(source) { coverBaseKey(source) }
    val initialPalette = remember(cacheKey) { cacheKey?.let { paletteCache.get(it) } }
    val paletteState = remember(cacheKey) { mutableStateOf<Palette?>(initialPalette) }
    val request = rememberPaletteImageRequest(source)
    val logger = rememberLogger("RememberPaletteState")

    LaunchedEffect(cacheKey) {
        val key = cacheKey ?: return@LaunchedEffect
        if (paletteState.value != null) {
            return@LaunchedEffect
        }

        // Palette generation is CPU bound; Coil performs its own IO internally.
        withContext(Dispatchers.Default) {
            val loader = SingletonImageLoader.get(context)

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
