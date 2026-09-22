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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.ColorUtils
import de.carsten.android.muzzic.visualization.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.visualization.VISUALIZER_BAR_SPACING
import de.carsten.android.muzzic.visualization.VISUALIZER_GLOW_INTENSITY
import de.carsten.android.muzzic.visualization.VISUALIZER_HUE_COLOR_DEGREE
import de.carsten.android.muzzic.visualization.VISUALIZER_LOG_BASE_DIVISOR
import de.carsten.android.muzzic.visualization.VISUALIZER_LOG_SCALE_FACTOR
import de.carsten.android.muzzic.visualization.VISUALIZER_SEGMENT_HEIGHT
import de.carsten.android.muzzic.visualization.VISUALIZER_SEGMENT_SPACING
import kotlin.math.ln

/**
 * Supported rendering engines for music visualization.
 */
enum class VisualizerEngine {
    /** 2D segmented bar graph drawn via Compose Canvas */
    BARS,

    /** 3D OpenGL Milkdrop visualizer powered by libprojectM */
    PROJECT_M,
}

/**
 * A real-time audio visualizer component supporting 2D segmented bars and 3D ProjectM Milkdrop visuals.
 *
 * @param amplitudesProvider Lambda providing the current list of normalized audio amplitudes (0.0 to 1.0).
 * @param modifier Modifier for the visualizer container.
 * @param engine Visualization rendering engine to use (default: [VisualizerEngine.BARS]).
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
    engine: VisualizerEngine = VisualizerEngine.BARS,
    color: Color = MaterialTheme.colorScheme.primary,
    isPlaying: Boolean = true,
    segmentHeight: Dp = VISUALIZER_SEGMENT_HEIGHT,
    segmentSpacing: Dp = VISUALIZER_SEGMENT_SPACING,
    barSpacing: Dp = VISUALIZER_BAR_SPACING,
) {
    when (engine) {
        VisualizerEngine.BARS -> {
            SegmentedBarsVisualizer(
                amplitudesProvider = amplitudesProvider,
                modifier = modifier,
                color = color,
                isPlaying = isPlaying,
                segmentHeight = segmentHeight,
                segmentSpacing = segmentSpacing,
                barSpacing = barSpacing,
            )
        }

        VisualizerEngine.PROJECT_M -> {
            AndroidView(
                factory = { context ->
                    ProjectMGLSurfaceView(context)
                },
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun SegmentedBarsVisualizer(
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
            this[0] = (this[0] + VISUALIZER_HUE_COLOR_DEGREE.toFloat()) % 360f
        }
        Color(ColorUtils.HSLToColor(targetHsl)).copy(alpha = color.alpha)
    }

    Canvas(modifier = modifier) {
        val amplitudes = amplitudesProvider()
        val barCount = amplitudes.size.coerceAtLeast(1)
        if (barCount == 0) return@Canvas

        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val maxSegmentsPerSide = (centerY / totalSegStepPx).toInt().coerceAtLeast(1)

        val barWidth = (width - (barCount - 1) * barSpacingPx) / barCount

        // Pre-calculate base colors for each segment level to avoid repeated lerp calls
        val segmentBaseColors = Array(maxSegmentsPerSide) { j ->
            lerp(color, targetColor, j.toFloat() / maxSegmentsPerSide.toFloat())
        }

        for (i in 0 until barCount) {
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

            if (rawAmplitude <= 0f) continue

            val scaledAmplitude = (ln((rawAmplitude * VISUALIZER_LOG_SCALE_FACTOR) + 1f) / ln(VISUALIZER_LOG_BASE_DIVISOR)).coerceIn(0f, 1f)
            val activeSegments = (scaledAmplitude * maxSegmentsPerSide).toInt()
            if (activeSegments <= 0) continue

            val x = i * (barWidth + barSpacingPx)
            val glowAmount = scaledAmplitude * VISUALIZER_GLOW_INTENSITY

            for (j in 0 until activeSegments) {
                val baseColor = segmentBaseColors[j]
                val finalColor = if (glowAmount > 0f) lerp(baseColor, Color.White, glowAmount) else baseColor

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
@Preview(uiMode = PREVIEW_DARK_MODE, name = "Dark Mode")
fun MusicVisualizationPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
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
