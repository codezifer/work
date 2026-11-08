package de.carsten.android.muzzic.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import coil3.ImageLoader
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.size.Precision
import coil3.toBitmap
import com.google.common.util.concurrent.ListenableFuture
import de.carsten.android.muzzic.ALBUMART_SCHEME
import de.carsten.android.muzzic.NOTIFICATION_ARTWORK_PX
import de.carsten.android.muzzic.model.AlbumArtUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.guava.future

/**
 * A [BitmapLoader] that delegates image loading to Coil.
 *
 * This bridge allows Media3 (specifically the [androidx.media3.session.MediaLibrarySession])
 * to use Coil's powerful image loading pipeline, including custom schemes like `albumart://`
 * via the `AlbumArtFetcher`.
 *
 * @param context The application context.
 * @param imageLoader The Coil [ImageLoader] instance.
 */
@UnstableApi
class CoilBitmapLoader(private val context: Context, private val imageLoader: ImageLoader) : BitmapLoader {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun supportsMimeType(mimeType: String): Boolean = true

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> = scope.future {
        BitmapFactory.decodeByteArray(data, 0, data.size)
            ?: throw IllegalArgumentException("Could not decode bitmap from byte array")
    }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> = scope.future {
        val data = uri.toString()
        // Share the cover's content key so the notification reuses an already decoded player cover.
        val coverKey = if (data.startsWith(ALBUMART_SCHEME)) AlbumArtUri.parse(data).contentKey() else null
        val request =
            ImageRequest
                .Builder(context)
                .data(data)
                .size(NOTIFICATION_ARTWORK_PX)
                .precision(Precision.INEXACT)
                // Read an already decoded player cover, but never replace it with the smaller icon.
                .memoryCachePolicy(CachePolicy.READ_ONLY)
                .apply {
                    if (coverKey != null) {
                        memoryCacheKey(coverKey)
                        diskCacheKey(coverKey)
                    }
                }
                .build()

        when (val result = imageLoader.execute(request)) {
            is SuccessResult -> result.image.toBitmap()
            is ErrorResult -> throw result.throwable
        }
    }
}
