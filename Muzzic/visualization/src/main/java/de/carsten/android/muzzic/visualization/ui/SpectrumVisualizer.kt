package de.carsten.android.muzzic.visualization.ui

import android.app.ActivityManager
import android.content.Context
import android.graphics.SurfaceTexture
import android.view.TextureView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import de.carsten.android.muzzic.visualization.GL_ES_3_0_VERSION_CODE
import de.carsten.android.muzzic.visualization.VisualizerConfig
import de.carsten.android.muzzic.visualization.VisualizerTheme
import de.carsten.android.muzzic.visualization.audio.SpectrumProcessor
import de.carsten.android.muzzic.visualization.bus.SpectrumBus
import de.carsten.android.muzzic.visualization.render.EglManager
import de.carsten.android.muzzic.visualization.render.LedBarRenderer
import de.carsten.android.muzzic.visualization.render.RenderStyle

/**
 * Real-time LED bar spectrum visualizer Composable.
 *
 * Hosts a `TextureView` (composited inside the app window, so transparency reveals
 * the app UI beneath and Compose clip/alpha apply) when GLES 3.0 is available,
 * or falls back seamlessly to [CanvasFallbackVisualizer] on legacy hardware
 * or EGL initialization failure.
 *
 * @param bus Audio spectrum ring buffer.
 * @param isPlaying Whether audio playback is currently active.
 * @param modifier Composable layout modifier.
 * @param config Visualizer configuration.
 * @param theme Color theme and zone definition.
 * @param style Render style: LED towers or mirrored classic bars.
 * @param processor Optional audio processor to toggle active state on pause/stop.
 */
@Composable
fun SpectrumVisualizer(
    bus: SpectrumBus,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    config: VisualizerConfig = VisualizerConfig(),
    theme: VisualizerTheme = VisualizerTheme.ClassicGreen,
    style: RenderStyle = RenderStyle.LED,
    processor: SpectrumProcessor? = null,
) {
    val context = LocalContext.current
    val isGlEs3Supported = remember(context) { checkGlEs3Support(context) }

    if (!isGlEs3Supported) {
        CanvasFallbackVisualizer(
            bus = bus,
            isPlaying = isPlaying,
            modifier = modifier,
            config = config,
            theme = theme,
        )
        return
    }

    val renderer = remember(bus) {
        LedBarRenderer(bus = bus, config = config, theme = theme, style = style)
    }

    LaunchedEffect(config, theme, style) {
        renderer.config = config
        renderer.theme = theme
        renderer.style = style
    }

    var glFailed by remember { mutableStateOf(false) }
    if (glFailed) {
        CanvasFallbackVisualizer(
            bus = bus,
            isPlaying = isPlaying,
            modifier = modifier,
            config = config,
            theme = theme,
        )
        return
    }

    val eglManager = remember { EglManager() }
    var renderDriver by remember { mutableStateOf<RenderDriver?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    // Surface callbacks fire asynchronously; always read the latest value.
    val currentIsPlaying by rememberUpdatedState(isPlaying)

    AndroidView(
        factory = { ctx ->
            TextureView(ctx).apply {
                // Transparent compositing: unlit areas reveal the app UI beneath.
                isOpaque = false
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                        if (!eglManager.init(surface, width, height)) {
                            glFailed = true
                            return
                        }
                        renderer.onSurfaceCreated()
                        renderer.onSurfaceChanged(width, height)
                        val driver = RenderDriver(
                            requestRender = {
                                if (eglManager.isReady) {
                                    renderer.onDrawFrame()
                                    eglManager.swapBuffers()
                                }
                            },
                            renderer = renderer,
                            processor = processor,
                        )
                        renderDriver = driver
                        driver.isPlaying = currentIsPlaying
                    }

                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                        renderer.onSurfaceChanged(width, height)
                    }

                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        renderDriver?.stop()
                        renderDriver = null
                        renderer.release()
                        eglManager.release()
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
                }
            }
        },
        update = {
            renderDriver?.isPlaying = isPlaying
        },
        onRelease = {
            renderDriver?.stop()
            renderDriver = null
            renderer.release()
            eglManager.release()
        },
        modifier = modifier.fillMaxSize(),
    )

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                processor?.enabled?.set(false)
                renderDriver?.stop()
            } else if (event == Lifecycle.Event.ON_RESUME) {
                processor?.enabled?.set(true)
                if (isPlaying) {
                    renderDriver?.start()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

private fun checkGlEs3Support(context: Context): Boolean {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
    val info = am.deviceConfigurationInfo ?: return false
    return info.reqGlEsVersion >= GL_ES_3_0_VERSION_CODE
}
