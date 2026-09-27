package de.carsten.android.muzzic.visualization.component

import androidx.compose.ui.graphics.Color
import de.carsten.android.muzzic.visualization.BARS_SEGMENT_COUNT
import de.carsten.android.muzzic.visualization.DEFAULT_SHIMMER_STRENGTH
import de.carsten.android.muzzic.visualization.DEFAULT_TIP_GLOW_STRENGTH
import de.carsten.android.muzzic.visualization.VisualizerConfig
import de.carsten.android.muzzic.visualization.VisualizerTheme
import de.carsten.android.muzzic.visualization.render.RenderStyle

/**
 * Resolved rendering target for a [VisualizerEngine].
 *
 * The host composable branches over this sealed type instead of the engine enum,
 * so engine-specific setup (BARS gradient config vs. LED theme vs. ProjectM preset)
 * lives in [VisualizerFactory] and stays unit-testable without Compose.
 */
sealed interface VisualizerDefinition {
    /**
     * Spectrum-based GLES rendering shared by BARS and LED_SPECTRUM.
     *
     * @property config Visualizer configuration.
     * @property theme Color theme and zone definition.
     * @property style Render style: LED towers or mirrored classic bars.
     */
    data class Spectrum(val config: VisualizerConfig, val theme: VisualizerTheme, val style: RenderStyle) : VisualizerDefinition

    /**
     * External ProjectM rendering (no spectrum bus, no GLES shader pipeline).
     *
     * @property presetName Name of the selected ProjectM preset, if any.
     */
    data class ProjectM(val presetName: String? = null) : VisualizerDefinition
}

/**
 * Resolves a [VisualizerEngine] plus its [VisualizerParams] into a [VisualizerDefinition].
 */
object VisualizerFactory {
    /**
     * Resolves the rendering target for the given engine and parameters.
     *
     * @param engine Visualization rendering engine to use.
     * @param params Engine-specific parameters matching [engine].
     * @param color Base accent color (album-art derived) for the BARS gradient and LED zones.
     * @return resolved [VisualizerDefinition].
     * @throws IllegalArgumentException when [params] do not match [engine].
     */
    fun resolve(engine: VisualizerEngine, params: VisualizerParams, color: Color): VisualizerDefinition = when (engine) {
        VisualizerEngine.BARS -> {
            require(params is VisualizerParams.BarsParams) { "BARS requires BarsParams" }
            VisualizerDefinition.Spectrum(
                config = VisualizerConfig().copy(
                    segmentCount = BARS_SEGMENT_COUNT,
                    shimmerStrength = if (params.shimmerEnabled) DEFAULT_SHIMMER_STRENGTH else 0f,
                    tipGlowStrength = if (params.tipGlowEnabled) DEFAULT_TIP_GLOW_STRENGTH else 0f,
                ),
                theme = VisualizerTheme.barsThemeFrom(color),
                style = RenderStyle.MIRRORED_BARS,
            )
        }

        VisualizerEngine.LED_SPECTRUM -> {
            require(params is VisualizerParams.LedSpectrumParams) { "LED_SPECTRUM requires LedSpectrumParams" }
            VisualizerDefinition.Spectrum(
                config = VisualizerConfig(),
                theme = VisualizerTheme.ledThemeFrom(color),
                style = RenderStyle.LED,
            )
        }

        VisualizerEngine.PROJECT_M -> {
            require(params is VisualizerParams.ProjectMParams) { "PROJECT_M requires ProjectMParams" }
            VisualizerDefinition.ProjectM(presetName = params.presetName)
        }
    }
}
