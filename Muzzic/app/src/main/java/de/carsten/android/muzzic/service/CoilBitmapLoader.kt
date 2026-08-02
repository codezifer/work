package de.carsten.android.muzzic.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import coil3.ImageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import com.google.common.util.concurrent.ListenableFuture
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
        val request =
            ImageRequest
                .Builder(context)
                .data(uri.toString())
                .build()

        when (val result = imageLoader.execute(request)) {
            is SuccessResult -> result.image.toBitmap()
            is ErrorResult -> throw result.throwable
        }
    }
}
