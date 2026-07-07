package de.carsten.android.muzzic.ui.component

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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.SPACING_LARGE
import de.carsten.android.muzzic.ui.VISUALIZER_BAR_SPACING
import de.carsten.android.muzzic.ui.VISUALIZER_SEGMENT_HEIGHT
import de.carsten.android.muzzic.ui.VISUALIZER_SEGMENT_SPACING
import de.carsten.android.muzzic.ui.state.MusicVisualizerState
import de.carsten.android.muzzic.ui.theme.AppTheme

/**
 * A real-time audio visualizer component featuring segmented bars, horizontal symmetry,
 * and vertical color gradients.
 *
 * This component uses a [de.carsten.android.muzzic.ui.state.MusicVisualizerState] to handle the mathematical transformations,
 * following the Android State Holder pattern for UI logic.
 *
 * @param amplitudes List of normalized audio amplitudes (0.0 to 1.0).
 * @param modifier Modifier for the visualizer container.
 * @param color The base accent color for the visualization.
 * @param isPlaying Whether the visualization is currently active.
 * @param segmentHeight Height of each individual segment in a bar.
 * @param segmentSpacing Vertical spacing between segments.
 * @param barSpacing Horizontal spacing between bars.
 */
@Composable
fun MusicVisualization(
    amplitudes: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    isPlaying: Boolean = true,
    segmentHeight: Dp = VISUALIZER_SEGMENT_HEIGHT,
    segmentSpacing: Dp = VISUALIZER_SEGMENT_SPACING,
    barSpacing: Dp = VISUALIZER_BAR_SPACING,
) {
    val barCount = amplitudes.size.coerceAtLeast(1)

    // Pre-calculate the target color for the gradient edges
    val targetColor = remember(color) {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color.toArgb(), hsl)
        val targetHsl = hsl.copyOf().apply {
            this[0] = (this[0] + 180f) % 360f // Rotate hue by 180 degrees
        }
        Color(ColorUtils.HSLToColor(targetHsl))
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        val spacingPx = barSpacing.toPx()
        val barWidth = (width - (barCount - 1) * spacingPx) / barCount

        val segHeightPx = segmentHeight.toPx()
        val segSpacingPx = segmentSpacing.toPx()
        val totalSegStep = segHeightPx + segSpacingPx

        // Instantiate the state holder for calculation logic
        val state = MusicVisualizerState(
            barCount = barCount,
            maxSegmentsPerSide = (height / 2f) / totalSegStep,
            totalSegStepPx = totalSegStep,
            baseColor = color,
            targetColor = targetColor,
        )

        for (i in 0 until barCount) {
            val mirroredIndex = state.getMirroredIndex(i)
            val rawAmplitude = if (isPlaying && mirroredIndex < amplitudes.size) {
                amplitudes[mirroredIndex]
            } else {
                0f
            }

            val scaledAmplitude = state.getScaledAmplitude(rawAmplitude)
            val activeSegments = state.getActiveSegments(scaledAmplitude)
            val x = i * (barWidth + spacingPx)

            for (j in 0 until activeSegments) {
                val finalColor = state.getSegmentColor(j, scaledAmplitude)

                // Top segment
                drawRoundRect(
                    color = finalColor,
                    topLeft = Offset(x, centerY - (j + 1) * totalSegStep + segSpacingPx / 2f),
                    size = Size(barWidth, segHeightPx),
                    cornerRadius = CornerRadius(barWidth / 4f, barWidth / 4f),
                )

                // Bottom segment (mirrored vertically)
                drawRoundRect(
                    color = finalColor,
                    topLeft = Offset(x, centerY + j * totalSegStep + segSpacingPx / 2f),
                    size = Size(barWidth, segHeightPx),
                    cornerRadius = CornerRadius(barWidth / 4f, barWidth / 4f),
                )
            }
        }
    }
}

@Composable
@Preview(showBackground = true, name = "Light Mode")
@Preview(uiMode = PREVIEW_DARK_MODE, showBackground = true, name = "Dark Mode")
fun MusicVisualizationPreview() {
    AppTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SPACING_LARGE)
                .height(100.dp),
        ) {
            MusicVisualization(
                amplitudes = listOf(
                    0.1f, 0.4f, 0.8f, 0.3f, 0.6f, 0.9f, 0.2f, 0.5f,
                    0.7f, 0.4f, 0.3f, 0.8f, 0.5f, 0.2f, 0.6f, 0.4f,
                ),
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.primary,
                isPlaying = true,
            )
        }
    }
}
