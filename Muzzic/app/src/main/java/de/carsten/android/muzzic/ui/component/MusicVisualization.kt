package de.carsten.android.muzzic.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.SPACING_LARGE
import de.carsten.android.muzzic.ui.theme.AppTheme

/**
 * A real-time audio visualizer component with detailed movement, glowing neon effects,
 * and a dynamic color spectrum based on the accent color.
 */
@Composable
fun MusicVisualization(amplitudes: List<Float>, modifier: Modifier = Modifier, color: Color = Color.White.copy(alpha = 0.3f), isPlaying: Boolean = true) {
    val barCount = amplitudes.size.takeIf { it > 0 } ?: 32
    val spacing = 2.dp

    // Calculate a complementary "target" color for the high-end spectrum
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(color.toArgb(), hsl)
    val targetHsl = hsl.copyOf().apply {
        this[0] = (this[0] + 180f) % 360f // Rotate hue by 180 degrees
    }
    val targetColor = Color(ColorUtils.HSLToColor(targetHsl))

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val spacingPx = spacing.toPx()
        val barWidth = (width - (barCount - 1) * spacingPx) / barCount

        for (i in 0 until barCount) {
            val rawAmplitude = if (isPlaying && i < amplitudes.size) amplitudes[i] else 0f

            // Dynamic color interpolation across the frequency spectrum
            // Left (low) = base accent color, Right (high) = complementary color
            val baseBarColor = lerp(color, targetColor, i.toFloat() / barCount)

            // Brighten the color based on amplitude for the "glow"
            val brightenedColor = lerp(baseBarColor, Color.White, rawAmplitude * 0.3f)

            val barHeight = (rawAmplitude * height).coerceAtLeast(4.dp.toPx())
            val x = i * (barWidth + spacingPx)
            val y = (height - barHeight) / 2

            // 1. Dynamic Glow / Bloom (Pulses with amplitude)
            val glowAlpha = 0.25f * rawAmplitude.coerceAtLeast(0.1f)
            val glowExpand = 6.dp.toPx() * rawAmplitude

            drawRoundRect(
                color = brightenedColor.copy(alpha = glowAlpha),
                topLeft = Offset(x - glowExpand, y - glowExpand),
                size = Size(barWidth + glowExpand * 2f, barHeight + glowExpand * 2f),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )

            // 2. Main Bar with High-Intensity Neon Gradient
            val barBrush = Brush.verticalGradient(
                colors = listOf(
                    brightenedColor.copy(alpha = 0.6f),
                    brightenedColor, // Brightest point
                    brightenedColor.copy(alpha = 0.6f),
                ),
            )

            drawRoundRect(
                brush = barBrush,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 4f, barWidth / 4f),
            )

            // 3. Hot Core / Shine
            if (barWidth > 2.dp.toPx()) {
                val coreAlpha = 0.6f * rawAmplitude.coerceAtLeast(0.3f)
                drawRoundRect(
                    color = Color.White.copy(alpha = coreAlpha),
                    topLeft = Offset(x + barWidth * 0.25f, y + barHeight * 0.15f),
                    size = Size(barWidth * 0.5f, barHeight * 0.7f),
                    cornerRadius = CornerRadius(barWidth / 8f, barWidth / 8f),
                )
            }

            // 4. Sharp Outline (Neon tube effect)
            drawRoundRect(
                color = brightenedColor.copy(alpha = 0.8f),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 4f, barWidth / 4f),
                style = Stroke(width = 1.dp.toPx()),
            )
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
                amplitudes = listOf(0.1f, 0.4f, 0.8f, 0.3f, 0.6f, 0.9f, 0.2f, 0.5f, 0.7f, 0.4f, 0.3f, 0.8f, 0.5f, 0.2f, 0.6f, 0.4f),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                isPlaying = true,
            )
        }
    }
}
