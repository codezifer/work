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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.SPACING_LARGE
import de.carsten.android.muzzic.ui.VISUALIZER_BAR_SPACING
import de.carsten.android.muzzic.ui.VISUALIZER_GLOW_INTENSITY
import de.carsten.android.muzzic.ui.VISUALIZER_HUE_COLOR_DEGREE
import de.carsten.android.muzzic.ui.VISUALIZER_LOG_BASE_DIVISOR
import de.carsten.android.muzzic.ui.VISUALIZER_LOG_SCALE_FACTOR
import de.carsten.android.muzzic.ui.VISUALIZER_SEGMENT_HEIGHT
import de.carsten.android.muzzic.ui.VISUALIZER_SEGMENT_SPACING
import de.carsten.android.muzzic.ui.theme.AppTheme
import kotlin.math.ln

/**
 * A real-time audio visualizer component featuring segmented bars, horizontal symmetry,
 * and vertical color gradients.
 *
 * This component handles the mathematical transformations directly within the drawing phase
 * to minimize allocations and maximize performance during high-frequency audio updates.
 *
 * @param amplitudesProvider Lambda providing the current list of normalized audio amplitudes (0.0 to 1.0).
 * @param modifier Modifier for the visualizer container.
 * @param color The base accent color for the visualization.
 * @param isPlaying Whether the visualization is currently active.
 * @param segmentHeight Height of each individual segment in a bar.
 * @param segmentSpacing Vertical spacing between segments.
 * @param barSpacing Horizontal spacing between bars.
 */
@Composable
fun MusicVisualization(
    amplitudesProvider: () -> List<Float>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    isPlaying: Boolean = true,
    segmentHeight: Dp = VISUALIZER_SEGMENT_HEIGHT,
    segmentSpacing: Dp = VISUALIZER_SEGMENT_SPACING,
    barSpacing: Dp = VISUALIZER_BAR_SPACING,
) {
    val density = LocalDensity.current
    val segHeightPx = remember(density, segmentHeight) { with(density) { segmentHeight.toPx() } }
    val segSpacingPx = remember(density, segmentSpacing) { with(density) { segmentSpacing.toPx() } }
    val barSpacingPx = remember(density, barSpacing) { with(density) { barSpacing.toPx() } }
    val totalSegStepPx = segHeightPx + segSpacingPx

    // Pre-calculate the target color for the gradient edges
    val targetColor = remember(color) {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color.toArgb(), hsl)
        val targetHsl = hsl.copyOf().apply {
            this[0] = (this[0] + VISUALIZER_HUE_COLOR_DEGREE.toFloat()) % 360f // Rotate hue by specified degrees
        }
        Color(ColorUtils.HSLToColor(targetHsl)).copy(alpha = color.alpha)
    }

    Canvas(modifier = modifier) {
        val amplitudes = amplitudesProvider()
        val barCount = amplitudes.size.coerceAtLeast(1)

        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val maxSegmentsPerSide = centerY / totalSegStepPx

        val barWidth = (width - (barCount - 1) * barSpacingPx) / barCount

        // We use a simplified calculation here instead of re-allocating a State object
        // to avoid GC pressure during high-frequency audio updates.
        for (i in 0 until barCount) {
            // Horizontal Symmetry Logic
            val mirroredIndex = if (i < barCount / 2) {
                ((barCount / 2) - 1) - i
            } else {
                i - barCount / 2
            }

            val rawAmplitude = if (isPlaying && mirroredIndex < amplitudes.size) {
                amplitudes[mirroredIndex]
            } else {
                0f
            }

            // Logarithmic Scaling
            val scaledAmplitude = if (rawAmplitude > 0f) {
                (
                    ln((rawAmplitude * VISUALIZER_LOG_SCALE_FACTOR) + 1f) /
                        ln(VISUALIZER_LOG_BASE_DIVISOR)
                    ).coerceIn(0f, 1f)
            } else {
                0f
            }

            val activeSegments = (scaledAmplitude * maxSegmentsPerSide).toInt().coerceAtLeast(0)
            val x = i * (barWidth + barSpacingPx)

            for (j in 0 until activeSegments) {
                // Color Interpolation
                val gradientFactor = j.toFloat() / maxSegmentsPerSide.coerceAtLeast(1f)
                val baseColor = lerp(color, targetColor, gradientFactor)
                val finalColor = lerp(baseColor, Color.White, scaledAmplitude * VISUALIZER_GLOW_INTENSITY)

                // Top segment
                drawRoundRect(
                    color = finalColor,
                    topLeft = Offset(x, centerY - (j + 1) * totalSegStepPx + segSpacingPx / 2f),
                    size = Size(barWidth, segHeightPx),
                    cornerRadius = CornerRadius(barWidth / 4f, barWidth / 4f),
                )

                // Bottom segment (mirrored vertically)
                drawRoundRect(
                    color = finalColor,
                    topLeft = Offset(x, centerY + j * totalSegStepPx + segSpacingPx / 2f),
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
                amplitudesProvider = {
                    listOf(
                        0.1f, 0.4f, 0.8f, 0.3f, 0.6f, 0.9f, 0.2f, 0.5f,
                        0.7f, 0.4f, 0.3f, 0.8f, 0.5f, 0.2f, 0.6f, 0.4f,
                    )
                },
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.primary,
                isPlaying = true,
            )
        }
    }
}
