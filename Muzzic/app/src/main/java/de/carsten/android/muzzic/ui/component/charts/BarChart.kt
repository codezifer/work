package de.carsten.android.muzzic.ui.component.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

enum class BarChartOrientation { Vertical, Horizontal }

/**
 * A custom Bar Chart component drawn using Compose Canvas.
 *
 * @param data The values to be represented in the chart.
 * @param colors The colors corresponding to each data point. If one color is provided, it's used for all.
 * @param modifier The modifier to be applied to the chart.
 * @param labels Optional labels for each data point.
 * @param showAxis Whether to show the numeric axis scale.
 * @param axisSteps Number of steps on the numeric axis.
 * @param orientation The orientation of the bars (Vertical or Horizontal).
 * @param barSpacing The spacing between bars.
 * @param cornerRadius The corner radius for the bars.
 * @param textStyle The style for labels and axis text.
 */
@Composable
fun MuzzicBarChart(
    data: List<Float>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    labels: List<String>? = null,
    showAxis: Boolean = false,
    axisSteps: Int = 5,
    orientation: BarChartOrientation = BarChartOrientation.Vertical,
    barSpacing: Dp = 8.dp,
    cornerRadius: Dp = 4.dp,
    labelSpacing: Dp = 20.dp,
    axisSpacing: Dp = 40.dp,
    textStyle: TextStyle = MaterialTheme.typography.labelSmall,
) {
    if (data.isEmpty()) return
    val maxVal = data.maxOrNull() ?: 0f
    if (maxVal == 0f) return

    val textMeasurer = rememberTextMeasurer()
    val textColor = textStyle.color.takeIf { it != Color.Unspecified } ?: MaterialTheme.colorScheme.onSurface

    Canvas(modifier = modifier) {
        val spacingPx = barSpacing.toPx()
        val cornerRadiusPx = cornerRadius.toPx()

        // Padding for labels and axis
        val labelPaddingPx = labelSpacing.toPx()
        val axisPaddingPx = if (showAxis) axisSpacing.toPx() else 0f

        if (orientation == BarChartOrientation.Vertical) {
            val chartWidth = size.width - axisPaddingPx
            val chartHeight = size.height - labelPaddingPx
            val barWidth = (chartWidth - (spacingPx * (data.size - 1))) / data.size

            // Draw axis scale
            if (showAxis) {
                for (i in 0..axisSteps) {
                    val ratio = i.toFloat() / axisSteps
                    val y = chartHeight - (ratio * chartHeight)
                    val value = (ratio * maxVal).roundToInt()

                    drawLine(
                        color = textColor.copy(alpha = 0.1f),
                        start = Offset(axisPaddingPx, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                    )

                    val measuredValue = textMeasurer.measure(value.toString(), textStyle)
                    drawText(
                        textMeasurer = textMeasurer,
                        text = value.toString(),
                        style = textStyle.copy(color = textColor.copy(alpha = 0.8f)),
                        topLeft = Offset(axisPaddingPx - measuredValue.size.width - 8.dp.toPx(), y - measuredValue.size.height / 2f),
                    )
                }
            }

            // Draw bars and item labels
            data.forEachIndexed { index, value ->
                val barHeight = (value / maxVal) * chartHeight
                val color = colors.getOrElse(index) { colors.firstOrNull() ?: Color.Gray }
                val left = axisPaddingPx + index * (barWidth + spacingPx)

                drawRoundRect(
                    color = color,
                    topLeft = Offset(left, chartHeight - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                )

                labels?.getOrNull(index)?.let { label ->
                    val measuredText = textMeasurer.measure(label, textStyle)
                    drawText(
                        textMeasurer = textMeasurer,
                        text = label,
                        style = textStyle.copy(color = textColor),
                        topLeft = Offset(
                            left + (barWidth - measuredText.size.width) / 2,
                            chartHeight + 4.dp.toPx(),
                        ),
                    )
                }
            }
        } else {
            val chartHeight = size.height - axisPaddingPx
            val barHeight = (chartHeight - (spacingPx * (data.size - 1))) / data.size

            // Draw axis scale (bottom X-axis)
            if (showAxis) {
                for (i in 0..axisSteps) {
                    val ratio = i.toFloat() / axisSteps
                    val x = labelPaddingPx + (ratio * (size.width - labelPaddingPx))
                    val value = (ratio * maxVal).roundToInt()

                    drawLine(
                        color = textColor.copy(alpha = 0.1f),
                        start = Offset(x, 0f),
                        end = Offset(x, chartHeight),
                        strokeWidth = 1.dp.toPx(),
                    )

                    val measuredValue = textMeasurer.measure(value.toString(), textStyle)
                    drawText(
                        textMeasurer = textMeasurer,
                        text = value.toString(),
                        style = textStyle.copy(color = textColor.copy(alpha = 0.8f)),
                        topLeft = Offset(x - measuredValue.size.width / 2f, chartHeight + 4.dp.toPx()),
                    )
                }
            }

            // Draw bars and item labels
            data.forEachIndexed { index, value ->
                val barWidth = (value / maxVal) * (size.width - labelPaddingPx)
                val color = colors.getOrElse(index) { colors.firstOrNull() ?: Color.Gray }
                val top = index * (barHeight + spacingPx)

                drawRoundRect(
                    color = color,
                    topLeft = Offset(labelPaddingPx, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                )

                labels?.getOrNull(index)?.let { label ->
                    val measuredText = textMeasurer.measure(label, textStyle)
                    drawText(
                        textMeasurer = textMeasurer,
                        text = label,
                        style = textStyle.copy(color = textColor),
                        topLeft = Offset(
                            labelPaddingPx - measuredText.size.width - 4.dp.toPx(),
                            top + (barHeight - measuredText.size.height) / 2,
                        ),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun MuzzicBarChartVerticalWithLabelsPreview() {
    Column(Modifier.padding(16.dp)) {
        MuzzicBarChart(
            data = listOf(10f, 50f, 30f, 80f, 20f),
            colors = listOf(Color.Magenta),
            labels = listOf("Jan", "Feb", "Mar", "Apr", "May"),
            showAxis = true,
            modifier = Modifier.size(300.dp, 200.dp),
            orientation = BarChartOrientation.Vertical,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun MuzzicBarChartSharpPreview() {
    Column(Modifier.padding(16.dp)) {
        MuzzicBarChart(
            data = listOf(40f, 80f, 60f),
            colors = listOf(Color.Blue),
            cornerRadius = 0.dp,
            modifier = Modifier.size(200.dp, 100.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun MuzzicBarChartRoundedPreview() {
    Column(Modifier.padding(16.dp)) {
        MuzzicBarChart(
            data = listOf(40f, 80f, 60f),
            colors = listOf(Color.Green),
            cornerRadius = 50.dp,
            modifier = Modifier.size(200.dp, 100.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun MuzzicBarChartHorizontalWithLabelsPreview() {
    Column(Modifier.padding(16.dp)) {
        MuzzicBarChart(
            data = listOf(10f, 50f, 30f, 80f, 20f),
            colors = listOf(Color.Cyan),
            labels = listOf("A", "B", "C", "D", "E"),
            showAxis = true,
            modifier = Modifier.size(300.dp, 200.dp),
            orientation = BarChartOrientation.Horizontal,
        )
    }
}
