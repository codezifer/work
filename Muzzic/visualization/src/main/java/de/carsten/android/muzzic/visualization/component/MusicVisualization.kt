package de.carsten.android.muzzic.visualization.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import de.carsten.android.muzzic.visualization.BARS_SEGMENT_COUNT
import de.carsten.android.muzzic.visualization.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.visualization.VisualizerConfig
import de.carsten.android.muzzic.visualization.VisualizerTheme
import de.carsten.android.muzzic.visualization.audio.SpectrumProcessor
import de.carsten.android.muzzic.visualization.bus.SpectrumBus
import de.carsten.android.muzzic.visualization.ui.SpectrumVisualizer
import kotlin.math.min

/**
 * A real-time audio visualizer component supporting mirrored GLES bars, 3D ProjectM
 * Milkdrop visuals, and the 80s HiFi GLES 3.0 LED spectrum analyzer.
 *
 * Both bar engines are fed by the shared [SpectrumBus] (logarithmic bands with
 * dB scaling, tilt compensation, and auto-gain); no legacy FFT path remains.
 *
 * Engine setup is resolved via [VisualizerFactory] into a [VisualizerDefinition];
 * this composable only hosts the resolved target.
 *
 * @param modifier Modifier for the visualizer container.
 * @param engine Visualization rendering engine to use (default: [VisualizerEngine.BARS]).
 * @param presetName Name of the selected ProjectM preset (if using [VisualizerEngine.PROJECT_M]).
 * @param spectrumBus Spectrum ring buffer instance for the GLES engines.
 * @param spectrumProcessor Audio processor instance for the GLES engines.
 * @param color The base accent color (album-art derived) for the BARS gradient and LED zones.
 * @param isPlaying Whether the visualization is currently active.
 * @param shimmerEnabled Whether lit BARS segments shimmer (no-op for other engines).
 * @param tipGlowEnabled Whether BARS tip segments get a white highlight (no-op for other engines).
 */
@Composable
fun MusicVisualization(
    modifier: Modifier = Modifier,
    engine: VisualizerEngine = VisualizerEngine.BARS,
    presetName: String? = null,
    spectrumBus: SpectrumBus? = null,
    spectrumProcessor: SpectrumProcessor? = null,
    color: Color = MaterialTheme.colorScheme.primary,
    isPlaying: Boolean = true,
    shimmerEnabled: Boolean = true,
    tipGlowEnabled: Boolean = true,
) {
    val params = remember(engine, presetName, shimmerEnabled, tipGlowEnabled) {
        when (engine) {
            VisualizerEngine.BARS -> VisualizerParams.BarsParams(shimmerEnabled, tipGlowEnabled)
            VisualizerEngine.LED_SPECTRUM -> VisualizerParams.LedSpectrumParams
            VisualizerEngine.PROJECT_M -> VisualizerParams.ProjectMParams(presetName)
        }
    }
    val definition = remember(engine, params, color) {
        VisualizerFactory.resolve(engine, params, color)
    }

    when (definition) {
        is VisualizerDefinition.Spectrum -> {
            if (spectrumBus != null) {
                SpectrumVisualizer(
                    bus = spectrumBus,
                    isPlaying = isPlaying,
                    modifier = modifier,
                    config = definition.config,
                    theme = definition.theme,
                    style = definition.style,
                    processor = spectrumProcessor,
                )
            } else {
                Box(modifier = modifier)
            }
        }

        is VisualizerDefinition.ProjectM -> {
            val preset = definition.presetName
            AndroidView(
                factory = { context ->
                    ProjectMGLSurfaceView(context).apply {
                        this.presetName = preset
                        this.isPlaying = isPlaying
                    }
                },
                update = { view ->
                    view.presetName = preset
                    view.isPlaying = isPlaying
                },
                modifier = modifier,
            )
        }
    }
}

@Composable
@Preview(showBackground = true, name = "Light Mode")
@Preview(uiMode = PREVIEW_DARK_MODE, name = "Dark Mode")
fun MusicVisualizationPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(100.dp),
        ) {
            // GLES needs a device GL context, so previews render a static mock
            // of LED bars (same geometry and zone colors, no audio or animation).
            StaticBarsPreview(
                modifier = Modifier.fillMaxSize(),
                config = VisualizerConfig(segmentCount = BARS_SEGMENT_COUNT),
                theme = VisualizerTheme.barsThemeFrom(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

/**
 * Static, preview-only mock of LED bars with fixed levels.
 *
 * Uses the same cell geometry, zone colors, and dimming as the GLES engines so
 * theme changes stay visible in previews; deliberately free of bus I/O,
 * smoothing, and animation.
 */
@Composable
private fun StaticBarsPreview(modifier: Modifier = Modifier, config: VisualizerConfig = VisualizerConfig(), theme: VisualizerTheme = VisualizerTheme.ClassicGreen) {
    val values = remember(config.bandCount) {
        FloatArray(config.bandCount) { i -> PREVIEW_BAR_VALUES[i % PREVIEW_BAR_VALUES.size] }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        val bandCount = config.bandCount
        val segCount = config.segmentCount
        val cellW = width / bandCount
        val cellH = height / segCount

        val halfW = config.ledHalfSize.first * cellW
        val halfH = config.ledHalfSize.second * cellH
        val cornerR = config.cornerRadius * min(cellW, cellH)

        drawRect(color = theme.background)

        for (b in 0 until bandCount) {
            val barVal = values[b]
            val xCenter = (b + 0.5f) * cellW

            for (s in 0 until segCount) {
                val t = (s + 0.5f) / segCount
                val isLit = (s + 0.5f) / segCount <= barVal
                if (!isLit && theme.offIntensity <= 0.001f) continue

                val zoneColor = when {
                    t < theme.zoneStart.first -> theme.colLow
                    t < theme.zoneStart.second -> theme.colMid
                    else -> theme.colHigh
                }
                val finalColor = if (isLit) zoneColor else zoneColor.copy(alpha = theme.offIntensity)

                drawRoundRect(
                    color = finalColor,
                    topLeft = Offset(xCenter - halfW, height - (s + 0.5f) * cellH - halfH),
                    size = Size(halfW * 2f, halfH * 2f),
                    cornerRadius = CornerRadius(cornerR, cornerR),
                )
            }
        }
    }
}

private val PREVIEW_BAR_VALUES = floatArrayOf(0.1f, 0.4f, 0.8f, 0.3f, 0.6f, 0.9f, 0.2f, 0.5f)
