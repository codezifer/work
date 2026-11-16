package de.carsten.android.muzzic.ui.image

import coil3.EventListener
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.Options
import coil3.request.SuccessResult
import coil3.size.Size
import coil3.size.SizeResolver
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

    override fun resolveSizeStart(request: ImageRequest, sizeResolver: SizeResolver) {
        if (isCover) StartupTrace.mark("cover.$variant.sizeStart")
    }

    override fun resolveSizeEnd(request: ImageRequest, size: Size) {
        if (isCover) {
            resolvedSize = "${size.width}x${size.height}"
            StartupTrace.mark("cover.$variant.size=$resolvedSize")
        }
    }

    override fun mapStart(request: ImageRequest, input: Any) {
        if (isCover) StartupTrace.mark("cover.$variant.mapStart")
    }

    override fun mapEnd(request: ImageRequest, output: Any) {
        if (isCover) StartupTrace.mark("cover.$variant.mapEnd")
    }

    override fun keyStart(request: ImageRequest, input: Any) {
        if (isCover) StartupTrace.mark("cover.$variant.keyStart")
    }

    override fun keyEnd(request: ImageRequest, output: String?) {
        if (isCover) StartupTrace.mark("cover.$variant.keyEnd")
    }

    override fun fetchStart(request: ImageRequest, fetcher: Fetcher, options: Options) {
        if (isCover) StartupTrace.mark("cover.$variant.fetchStart")
    }

    override fun fetchEnd(request: ImageRequest, fetcher: Fetcher, options: Options, result: FetchResult?) {
        if (isCover) {
            val source = (result as? SourceFetchResult)?.dataSource?.name ?: "-"
            StartupTrace.mark("cover.$variant.fetchEnd source=$source")
        }
    }

    override fun decodeStart(request: ImageRequest, decoder: Decoder, options: Options) {
        if (isCover) StartupTrace.mark("cover.$variant.decodeStart")
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
