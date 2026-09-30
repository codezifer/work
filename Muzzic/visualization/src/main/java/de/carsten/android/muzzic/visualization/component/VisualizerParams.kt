package de.carsten.android.muzzic.visualization.component

/**
 * Engine-specific parameters for music visualization.
 *
 * Each [VisualizerEngine] declares only the parameters it actually consumes, so
 * callers can no longer pass e.g. BARS shimmer flags to ProjectM as silent no-ops.
 */
sealed interface VisualizerParams {
    /**
     * Parameters for [VisualizerEngine.BARS].
     *
     * @property shimmerEnabled Whether lit BARS segments shimmer.
     * @property tipGlowEnabled Whether BARS tip segments get a white highlight.
     * @property hueRotationSpeed Free hue rotation in full turns per second (0 disables).
     */
    data class BarsParams(val shimmerEnabled: Boolean = true, val tipGlowEnabled: Boolean = true, val hueRotationSpeed: Float = 0f) : VisualizerParams

    /**
     * Parameters for [VisualizerEngine.LED_SPECTRUM].
     *
     * @property hueRotationSpeed Free hue rotation in full turns per second (0 disables).
     */
    data class LedSpectrumParams(val hueRotationSpeed: Float = 0f) : VisualizerParams

    /**
     * Parameters for [VisualizerEngine.PROJECT_M].
     *
     * @property presetName Name of the selected ProjectM preset, if any.
     */
    data class ProjectMParams(val presetName: String? = null) : VisualizerParams
}
