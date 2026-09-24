package de.carsten.android.muzzic.visualization.ui

import android.app.ActivityManager
import android.content.Context
import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import de.carsten.android.muzzic.visualization.render.LedBarRenderer
import de.carsten.android.muzzic.visualization.render.RenderStyle

/**
 * Real-time LED bar spectrum visualizer Composable.
 *
 * Automatically hosts a GLES 3.0 `GLSurfaceView` when supported, or falls back seamlessly to
 * [CanvasFallbackVisualizer] on legacy hardware.
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

    var renderDriver by remember { mutableStateOf<RenderDriver?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            GLSurfaceView(ctx).apply {
                setEGLContextClientVersion(3)
                if (style == RenderStyle.MIRRORED_BARS) {
                    // Translucent surface so the host layout shows through
                    // unlit areas (classic bars draw no background).
                    holder.setFormat(PixelFormat.TRANSLUCENT)
                    setEGLConfigChooser(8, 8, 8, 8, 16, 0)
                } else {
                    setEGLConfigChooser(8, 8, 8, 8, 0, 0)
                }
                setRenderer(renderer)
                renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
                preserveEGLContextOnPause = true
            }.also { view ->
                val driver = RenderDriver(view, renderer, processor)
                renderDriver = driver
            }
        },
        update = {
            renderDriver?.isPlaying = isPlaying
        },
        onRelease = { view ->
            renderDriver?.stop()
            renderDriver = null
            // GL resources must be deleted on the GL thread, not the UI thread.
            view.queueEvent { renderer.release() }
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
