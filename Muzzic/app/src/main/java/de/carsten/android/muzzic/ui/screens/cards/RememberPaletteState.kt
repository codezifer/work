package de.carsten.android.muzzic.ui.screens.cards

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
import de.carsten.android.muzzic.logging.AndroidLogger
import de.carsten.android.muzzic.logging.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberPaletteState(source: Any?): State<Palette?> {
    val context = LocalContext.current
    val paletteState = remember(source) { mutableStateOf<Palette?>(null) }
    val logger = remember { AndroidLogger.getLogger("RememberPaletteState") }

    LaunchedEffect(source) {
        paletteState.value = null // Reset immediately when source changes
        if (source == null) {
            return@LaunchedEffect
        }
        val loader = SingletonImageLoader.get(context)
        val request =
            ImageRequest
                .Builder(context)
                .data(source)
                .allowHardware(false) // Required for Palette to read pixels
                .build()

        val result = loader.execute(request)
        if (result is SuccessResult) {
            val bitmap = result.image.asDrawable(context.resources).toBitmap()
            withContext(Dispatchers.Default) {
                paletteState.value =
                    Palette
                        .from(bitmap)
                        .maximumColorCount(24) // Increase color count for better vibrant detection
                        .generate()
            }
        } else if (result is ErrorResult) {
            logger.error("Failed to load image for palette: $source", result.throwable)
        }
    }
    return paletteState
}
