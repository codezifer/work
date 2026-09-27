package de.carsten.android.muzzic.visualization.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import de.carsten.android.muzzic.visualization.BARS_SEGMENT_COUNT
import de.carsten.android.muzzic.visualization.DEFAULT_SPECTRUM_BANDS
import de.carsten.android.muzzic.visualization.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.visualization.PREVIEW_SYNTH_FRAME_AGE_NANOS
import de.carsten.android.muzzic.visualization.VisualizerConfig
import de.carsten.android.muzzic.visualization.VisualizerTheme
import de.carsten.android.muzzic.visualization.audio.SpectrumProcessor
import de.carsten.android.muzzic.visualization.bus.SpectrumBus
import de.carsten.android.muzzic.visualization.ui.CanvasFallbackVisualizer
import de.carsten.android.muzzic.visualization.ui.SpectrumVisualizer

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
                    }
                },
                update = { view ->
                    view.presetName = preset
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
            // Static preview through the Canvas fallback with a pre-filled bus.
            // The frame is backdated past the latency window so the fallback picks it up.
            val bus = remember {
                SpectrumBus().also { filled ->
                    val values = FloatArray(DEFAULT_SPECTRUM_BANDS) { i ->
                        listOf(0.1f, 0.4f, 0.8f, 0.3f, 0.6f, 0.9f, 0.2f, 0.5f)[i % 8]
                    }
                    filled.write(System.nanoTime() - PREVIEW_SYNTH_FRAME_AGE_NANOS, values, DEFAULT_SPECTRUM_BANDS)
                }
            }
            CanvasFallbackVisualizer(
                bus = bus,
                isPlaying = true,
                modifier = Modifier.fillMaxSize(),
                config = VisualizerConfig(segmentCount = BARS_SEGMENT_COUNT),
                theme = VisualizerTheme.barsThemeFrom(MaterialTheme.colorScheme.primary),
            )
        }
    }
}
