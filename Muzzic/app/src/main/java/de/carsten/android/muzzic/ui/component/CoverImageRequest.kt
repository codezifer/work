package de.carsten.android.muzzic.ui.component

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.size.Precision
import de.carsten.android.muzzic.ALBUMART_SCHEME
import de.carsten.android.muzzic.model.AlbumArtUri
import de.carsten.android.muzzic.ui.BLUR_BG_PX
import de.carsten.android.muzzic.ui.COVER_KEY_BLUR
import de.carsten.android.muzzic.ui.COVER_KEY_PALETTE
import de.carsten.android.muzzic.ui.PALETTE_SIZE_PX

/**
 * Remembers a Coil [ImageRequest] for cover artwork that is drawn on screen.
 *
 * No size is set, so Coil derives the target size from the measured layout bounds and stays
 * density-aware. [coverBaseKey] is used for memory, disk and placeholder-memory keys, so
 * scrolling back hits the memory cache synchronously. The key is content based, so every
 * track of an album shares one decoded bitmap.
 *
 * `data` is intentionally non-nullable: Coil fails a request without data before any
 * placeholder or error painter is applied, so a missing cover must be handled by the caller
 * (see [DefaultAlbumArt]).
 *
 * @param data the image data (cover path string, [Uri] or [AlbumArtUri]).
 * @return the remembered [ImageRequest].
 */
@Composable
fun rememberCoverImageRequest(data: Any): ImageRequest {
    val context = LocalContext.current
    return remember(data) {
        val key = coverBaseKey(data)
        ImageRequest
            .Builder(context)
            .data(data)
            .memoryCacheKey(key)
            .diskCacheKey(key)
            .placeholderMemoryCacheKey(key)
            .precision(Precision.INEXACT)
            .build()
    }
}

/**
 * Remembers a Coil [ImageRequest] used only for palette extraction.
 *
 * Decodes a small software bitmap: palette generation cannot read hardware bitmaps, and
 * colours do not need display resolution. The separate memory key keeps the small entry from
 * replacing the full-size display bitmap. The disk key also stays [coverBaseKey], so network
 * covers share one disk entry across all variants.
 *
 * @param data the image data (cover path string, [Uri] or [AlbumArtUri]).
 * @return the remembered [ImageRequest].
 */
@Composable
fun rememberPaletteImageRequest(data: Any): ImageRequest {
    val context = LocalContext.current
    return remember(data) {
        val key = coverBaseKey(data)
        ImageRequest
            .Builder(context)
            .data(data)
            .memoryCacheKey(key?.plus(COVER_KEY_PALETTE))
            .diskCacheKey(key)
            .size(PALETTE_SIZE_PX)
            .allowHardware(false)
            .precision(Precision.INEXACT)
            .build()
    }
}

/**
 * Remembers a Coil [ImageRequest] for blurred background covers.
 *
 * A blurred layer carries no fine detail, so a small decode is sufficient. Software
 * bitmap, because the hardware allocator disables ImageDecoder's sampling (0025). The separate
 * memory key keeps it from replacing the full-size display bitmap.
 *
 * @param data the image data (cover path string, [Uri] or [AlbumArtUri]).
 * @return the remembered [ImageRequest].
 */
@Composable
fun rememberBlurImageRequest(data: Any): ImageRequest {
    val context = LocalContext.current
    return remember(data) {
        val key = coverBaseKey(data)
        ImageRequest
            .Builder(context)
            .data(data)
            .memoryCacheKey(key?.plus(COVER_KEY_BLUR))
            .diskCacheKey(key)
            .size(BLUR_BG_PX)
            .allowHardware(false)
            .precision(Precision.INEXACT)
            .build()
    }
}

/**
 * Resolves the content identity used as Coil cache key for cover artwork.
 *
 * Album art URIs are keyed by their image content (see [AlbumArtUri.contentKey]) so that every
 * track of an album shares one decoded bitmap and one palette instead of holding a copy per
 * song file. Other strings (HTTP URLs, resource paths) are used as is.
 *
 * Only types with a stable identity are mapped. Anything else (e.g. an `InputStream`) returns
 * null so Coil falls back to its own keying instead of storing a per-instance identity hash.
 * A null [data] has no identity at all; callers treat that as "no cover" and skip the request.
 *
 * @param data the image source passed to Coil.
 * @return the stable key, or null when [data] has no stable identity.
 */
fun coverBaseKey(data: Any?): String? = when (data) {
    null -> null

    is AlbumArtUri -> data.contentKey() ?: data.get()

    is String ->
        if (data.startsWith(ALBUMART_SCHEME)) {
            AlbumArtUri.parse(data).contentKey() ?: data
        } else {
            data
        }

    is Uri -> data.toString()

    else -> null
}
