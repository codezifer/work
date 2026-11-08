package de.carsten.android.muzzic.ui.image

import coil3.EventListener
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.Options
import coil3.request.SuccessResult
import coil3.size.Size
import de.carsten.android.muzzic.ALBUMART_SCHEME
import de.carsten.android.muzzic.logging.StartupTrace
import de.carsten.android.muzzic.ui.COVER_KEY_BLUR
import de.carsten.android.muzzic.ui.COVER_KEY_PALETTE

/**
 * Logs cover request timings for the startup measurement.
 *
 * One instance is created per request, so the resolved size and variant stay attached to the
 * matching request even when display, palette, blur and notification load concurrently.
 */
class StartupImageListener(request: ImageRequest) : EventListener() {
    private val isCover = (request.data as? String)?.startsWith(ALBUMART_SCHEME) == true
    private val memoryKey = request.memoryCacheKey
    private val variant = when {
        memoryKey?.endsWith(COVER_KEY_PALETTE) == true -> "palette"

        memoryKey?.endsWith(COVER_KEY_BLUR) == true -> "blur"

        // Media3 loads the notification bitmap directly, so it has no Compose target.
        request.target == null -> "notification"

        else -> "display"
    }
    private var resolvedSize: String = "?"

    override fun onStart(request: ImageRequest) {
        if (isCover) StartupTrace.mark("cover.$variant.start")
    }

    override fun resolveSizeEnd(request: ImageRequest, size: Size) {
        if (isCover) {
            resolvedSize = "${size.width}x${size.height}"
            StartupTrace.mark("cover.$variant.size=$resolvedSize")
        }
    }

    override fun decodeEnd(request: ImageRequest, decoder: Decoder, options: Options, result: DecodeResult?) {
        if (isCover) StartupTrace.mark("cover.$variant.decodeEnd size=$resolvedSize")
    }

    override fun onSuccess(request: ImageRequest, result: SuccessResult) {
        if (isCover) StartupTrace.mark("cover.$variant.success size=$resolvedSize source=${result.dataSource}")
    }

    override fun onError(request: ImageRequest, result: ErrorResult) {
        if (isCover) StartupTrace.mark("cover.$variant.error size=$resolvedSize")
    }
}
