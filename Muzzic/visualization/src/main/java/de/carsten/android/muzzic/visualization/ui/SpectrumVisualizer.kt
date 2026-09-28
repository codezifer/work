package de.carsten.android.muzzic.visualization.ui

import android.app.ActivityManager
import android.content.Context
import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
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
 * All EGL and GL work runs on a dedicated `VisualizerGL` thread (one per live
 * surface): `SurfaceTextureListener` callbacks and the [RenderDriver] frame loop
 * only post there, so heavy fullscreen shader work never blocks the main thread.
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

    val renderer = remember {
        LedBarRenderer(bus = bus, config = config, theme = theme, style = style)
    }

    LaunchedEffect(bus, config, theme, style) {
        renderer.bus = bus
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
    var glHost by remember { mutableStateOf<GlHost?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    // Surface callbacks fire asynchronously; always read the latest values.
    val currentIsPlaying by rememberUpdatedState(isPlaying)
    val currentConfig by rememberUpdatedState(config)
    val currentProcessor by rememberUpdatedState(processor)
    val currentHost by rememberUpdatedState(glHost)

    /**
     * Stops the frame loop, releases GL resources on the GL thread, and shuts
     * the render thread down. Safe to call repeatedly and from any thread.
     */
    fun shutdownGlHost() {
        val host = glHost ?: return
        glHost = null
        host.handler.post {
            host.driver?.stop()
            host.driver = null
            renderer.release()
            eglManager.release()
        }
        host.thread.quitSafely()
    }

    AndroidView(
        factory = { ctx ->
            TextureView(ctx).apply {
                // Transparent compositing: unlit areas reveal the app UI beneath.
                isOpaque = false
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                        val thread = HandlerThread("VisualizerGL").apply { start() }
                        val host = GlHost(thread, Handler(thread.looper), eglManager)
                        glHost = host
                        host.handler.post {
                            if (!eglManager.init(surface, width, height)) {
                                Handler(Looper.getMainLooper()).post { glFailed = true }
                                return@post
                            }
                            host.width = width
                            host.height = height
                            renderer.onSurfaceCreated()
                            renderer.onSurfaceChanged(width, height)
                            val driver = RenderDriver(
                                requestRender = {
                                    renderer.onDrawFrame()
                                    eglManager.swapBuffers()
                                },
                                renderer = renderer,
                                processor = currentProcessor,
                                maxFps = currentConfig.maxFps,
                            )
                            host.driver = driver
                            // Start the loop unconditionally: with isPlaying=false it
                            // renders until bars settle, then auto-stops via isIdle.
                            driver.isPlaying = currentIsPlaying
                            driver.start()
                        }
                    }

                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                        val host = glHost ?: return
                        host.width = width
                        host.height = height
                        host.handler.post {
                            eglManager.setBufferSize(width, height)
                            renderer.onSurfaceChanged(width, height)
                        }
                    }

                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        shutdownGlHost()
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
                }
            }
        },
        update = {
            val host = glHost ?: return@AndroidView
            host.handler.post {
                host.driver?.let { driver ->
                    driver.isPlaying = isPlaying
                    driver.maxFps = config.maxFps
                }
            }
        },
        onRelease = {
            shutdownGlHost()
        },
        modifier = modifier.fillMaxSize(),
    )

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                currentProcessor?.enabled?.set(false)
                currentHost?.let { host ->
                    host.handler.post { host.driver?.stop() }
                }
            } else if (event == Lifecycle.Event.ON_RESUME) {
                currentProcessor?.enabled?.set(true)
                if (currentIsPlaying) {
                    currentHost?.let { host ->
                        host.handler.post { host.driver?.start() }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

/**
 * Render-thread host for one live GL surface: the thread, its handler, and the
 * frame driver. `width`/`height` track the last known surface size.
 */
private class GlHost(val thread: HandlerThread, val handler: Handler, val eglManager: EglManager) {
    @Volatile var driver: RenderDriver? = null

    @Volatile var width: Int = 0

    @Volatile var height: Int = 0
}

private fun checkGlEs3Support(context: Context): Boolean {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
    val info = am.deviceConfigurationInfo ?: return false
    return info.reqGlEsVersion >= GL_ES_3_0_VERSION_CODE
}
