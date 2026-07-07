package de.carsten.android.muzzic.ui.state

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import de.carsten.android.muzzic.ui.VISUALIZER_GLOW_INTENSITY
import de.carsten.android.muzzic.ui.VISUALIZER_LOG_BASE_DIVISOR
import de.carsten.android.muzzic.ui.VISUALIZER_LOG_SCALE_FACTOR
import kotlin.math.ln

/**
 * A state holder that encapsulates the mathematical logic for audio visualization.
 *
 * This follows the "State Holder" pattern recommended by Google for handling
 * complex UI logic separately from the Composable drawing code.
 *
 * @property barCount Total number of bars to be displayed.
 * @property maxSegmentsPerSide Maximum number of segments that fit on one side (top or bottom).
 * @property totalSegStepPx Height of one segment plus its spacing in pixels.
 * @property baseColor The primary color for the center of the visualization.
 * @property targetColor The color for the edges of the visualization.
 */
@Immutable
class MusicVisualizerState(val barCount: Int, val maxSegmentsPerSide: Float, val totalSegStepPx: Float, private val baseColor: Color, private val targetColor: Color) {
    /**
     * Calculates the mirrored index for horizontal symmetry.
     * Bass frequencies are placed in the center.
     */
    fun getMirroredIndex(index: Int): Int = if (index < barCount / 2) {
        ((barCount / 2) - 1) - index
    } else {
        index - barCount / 2
    }

    /**
     * Applies logarithmic scaling to a raw amplitude to improve dynamic range.
     */
    fun getScaledAmplitude(rawAmplitude: Float): Float = if (rawAmplitude > 0f) {
        (ln((rawAmplitude * VISUALIZER_LOG_SCALE_FACTOR) + 1f) / ln(VISUALIZER_LOG_BASE_DIVISOR)).coerceIn(0f, 1f)
    } else {
        0f
    }

    /**
     * Determines how many segments should be active based on the scaled amplitude.
     */
    fun getActiveSegments(scaledAmplitude: Float): Int = (scaledAmplitude * maxSegmentsPerSide).toInt().coerceAtLeast(1)

    /**
     * Interpolates the color for a specific segment and applies a glow effect.
     */
    fun getSegmentColor(segmentIndex: Int, scaledAmplitude: Float): Color {
        val gradientFactor = segmentIndex.toFloat() / maxSegmentsPerSide.coerceAtLeast(1f)
        val interpolated = lerp(baseColor, targetColor, gradientFactor)

        // Apply a subtle white glow based on amplitude
        return lerp(interpolated, Color.White, scaledAmplitude * VISUALIZER_GLOW_INTENSITY)
    }
}
