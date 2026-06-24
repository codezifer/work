package de.carsten.android.muzzic.ui.component.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import de.carsten.android.muzzic.ui.CHART_PREVIEW_HEIGHT_LARGE
import de.carsten.android.muzzic.ui.SPACING_LARGE
import kotlin.math.cos
import kotlin.math.sin

/**
 * A custom Pie/Donut Chart component drawn using Compose Canvas.
 *
 * @param data The values to be represented in the chart.
 * @param colors The colors corresponding to each data point.
 * @param modifier The modifier to be applied to the chart.
 * @param labels Optional labels for each data point.
 * @param holeRadiusPercent The radius of the hole in the center, from 0.0 (Pie) to 1.0.
 * @param textStyle The style for labels.
 */
@Composable
fun MuzzicPieChart(
    data: List<Float>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    labels: List<String>? = null,
    holeRadiusPercent: Float = 0.6f,
    textStyle: TextStyle = MaterialTheme.typography.labelSmall,
) {
    if (data.isEmpty()) return
    val total = data.sum()
    if (total == 0f) return

    val textMeasurer = rememberTextMeasurer()
    val textColor = textStyle.color.takeIf { it != Color.Unspecified } ?: MaterialTheme.colorScheme.onSurface

    Canvas(modifier = modifier) {
        var startAngle = -90f
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        data.forEachIndexed { index, value ->
            val sweepAngle = (value / total) * 360f
            val color = colors.getOrElse(index) { Color.Gray }

            if (holeRadiusPercent <= 0f) {
                // Pie Chart
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                )
            } else {
                // Donut Chart
                val strokeWidth = size.minDimension * (1f - holeRadiusPercent) / 2f
                val arcSize = Size(size.minDimension - strokeWidth, size.minDimension - strokeWidth)

                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth),
                    size = arcSize,
                    topLeft = Offset(
                        (size.width - arcSize.width) / 2f,
                        (size.height - arcSize.height) / 2f,
                    ),
                )
            }

            // Draw labels
            labels?.getOrNull(index)?.let { label ->
                val midAngle = startAngle + sweepAngle / 2f
                val midAngleRad = Math.toRadians(midAngle.toDouble())

                // Position label at 70% of radius (or in the middle of the ring for donut)
                val labelRadius = if (holeRadiusPercent <= 0f) {
                    radius * 0.7f
                } else {
                    radius * (holeRadiusPercent + (1f - holeRadiusPercent) / 2f)
                }

                val x = center.x + (labelRadius * cos(midAngleRad)).toFloat()
                val y = center.y + (labelRadius * sin(midAngleRad)).toFloat()

                val measuredText = textMeasurer.measure(label, textStyle)
                drawText(
                    textMeasurer = textMeasurer,
                    text = label,
                    style = textStyle.copy(color = textColor),
                    topLeft = Offset(x - measuredText.size.width / 2f, y - measuredText.size.height / 2f),
                )
            }

            startAngle += sweepAngle
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MuzzicDonutChartWithLabelsPreview() {
    Column(Modifier.padding(SPACING_LARGE)) {
        MuzzicPieChart(
            data = listOf(40f, 30f, 20f, 10f),
            colors = listOf(Color.Red, Color.Blue, Color.Green, Color.Yellow),
            labels = listOf("40%", "30%", "20%", "10%"),
            modifier = Modifier.size(CHART_PREVIEW_HEIGHT_LARGE),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MuzzicPieChartWithLabelsPreview() {
    Column(Modifier.padding(SPACING_LARGE)) {
        MuzzicPieChart(
            data = listOf(40f, 30f, 20f, 10f),
            colors = listOf(Color.Red, Color.Blue, Color.Green, Color.Yellow),
            labels = listOf("A", "B", "C", "D"),
            modifier = Modifier.size(CHART_PREVIEW_HEIGHT_LARGE),
            holeRadiusPercent = 0f,
        )
    }
}
