package de.carsten.android.muzzic.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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
import de.carsten.android.muzzic.ui.theme.AppTheme

/**
 * A real-time audio visualizer component featuring segmented bars, horizontal symmetry,
 * and vertical color gradients.
 *
 * The visualization is mirrored from the center, placing bass frequencies at the core
 * and treble at the outer edges.
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
    // Determine the number of bars to draw based on input amplitudes
    val barCount = amplitudes.size.coerceAtLeast(1)

    // Calculate a complementary "target" color for the vertical gradient edges
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(color.toArgb(), hsl)
    val targetHsl = hsl.copyOf().apply {
        this[0] = (this[0] + 180f) % 360f // Rotate hue by 180 degrees
    }
    val targetColor = Color(ColorUtils.HSLToColor(targetHsl))

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        val spacingPx = barSpacing.toPx()
        val barWidth = (width - (barCount - 1) * spacingPx) / barCount

        val segHeightPx = segmentHeight.toPx()
        val segSpacingPx = segmentSpacing.toPx()
        val totalSegStep = segHeightPx + segSpacingPx

        // Number of segments that can fit from center to one edge
        val maxSegmentsPerSide = (height / 2f) / totalSegStep

        for (i in 0 until barCount) {
            // Horizontal Symmetry Logic: Bass at the center, Treble at the edges
            val distanceFromCenter = if (i < barCount / 2) {
                (barCount / 2 - 1) - i
            } else {
                i - barCount / 2
            }

            // Get amplitude for the current mirrored position
            val rawAmplitude = if (isPlaying && distanceFromCenter < amplitudes.size) {
                amplitudes[distanceFromCenter]
            } else {
                0f
            }

            val x = i * (barWidth + spacingPx)
            val activeSegments = (rawAmplitude * maxSegmentsPerSide).toInt().coerceAtLeast(1)

            // Draw segments vertically from the center outwards
            for (j in 0 until activeSegments) {
                // Vertical Gradient: Interpolate color from center to edge
                val gradientFactor = j.toFloat() / maxSegmentsPerSide.coerceAtLeast(1f)
                val segmentColor = lerp(color, targetColor, gradientFactor)

                // Brighten the segment slightly based on amplitude for a glow effect
                val finalColor = lerp(segmentColor, Color.White, rawAmplitude * 0.2f)

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
