package de.carsten.android.muzzic.persistence.repo

import de.carsten.android.muzzic.visualization.component.VisualizerColorSource
import de.carsten.android.muzzic.visualization.component.VisualizerEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Snapshot of every visualizer-related app setting.
 *
 * @property engine active visualizer engine.
 * @property preset selected ProjectM preset name, or `null` when none is stored.
 * @property shimmerEnabled whether mirrored-BARS shimmer is enabled.
 * @property tipGlowEnabled whether the mirrored-BARS tip glow is enabled.
 * @property hueRotationSpeed free hue-rotation speed in full turns per second.
 * @property colorSource where the visualizer colors come from.
 */
data class VisualizerOptions(
    val engine: VisualizerEngine,
    val preset: String?,
    val shimmerEnabled: Boolean,
    val tipGlowEnabled: Boolean,
    val hueRotationSpeed: Float,
    val colorSource: VisualizerColorSource,
)

/**
 * Combines the six visualizer setting flows into a single stream.
 *
 * Uses only the individually observable settings, so callers never need to
 * combine them twice for settings and player screens.
 *
 * @return a [Flow] emitting a [VisualizerOptions] on every setting change.
 */
fun AppSettingsRepository.observeVisualizerOptions(): Flow<VisualizerOptions> = combine(
    observeVisualizerEngine(),
    observeProjectMPreset(),
    observeBarsShimmerEnabled(),
    observeBarsTipGlowEnabled(),
    // Nested (arity limit of typed combine): hue speed + color source as one slot.
    combine(observeHueRotationSpeed(), observeVisualizerColorSource()) { speed, source -> speed to source },
) { engine, preset, shimmerEnabled, tipGlowEnabled, (hueRotationSpeed, colorSource) ->
    VisualizerOptions(engine, preset, shimmerEnabled, tipGlowEnabled, hueRotationSpeed, colorSource)
}
