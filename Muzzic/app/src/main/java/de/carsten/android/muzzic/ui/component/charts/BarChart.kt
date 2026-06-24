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
import de.carsten.android.muzzic.ui.BORDER_WIDTH_NORMAL
import de.carsten.android.muzzic.ui.CHART_AXIS_SPACING
import de.carsten.android.muzzic.ui.CHART_FULL_ROUNDING
import de.carsten.android.muzzic.ui.CHART_LABEL_SPACING
import de.carsten.android.muzzic.ui.CHART_PREVIEW_HEIGHT_LARGE
import de.carsten.android.muzzic.ui.CHART_PREVIEW_HEIGHT_SMALL
import de.carsten.android.muzzic.ui.CHART_PREVIEW_WIDTH_LARGE
import de.carsten.android.muzzic.ui.CHART_PREVIEW_WIDTH_MEDIUM
import de.carsten.android.muzzic.ui.SPACING_LARGE
import de.carsten.android.muzzic.ui.SPACING_MEDIUM
import de.carsten.android.muzzic.ui.SPACING_SMALL
import de.carsten.android.muzzic.ui.SPACING_TINY
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
 * @param showValuesInside Whether to show the numeric values inside the bars.
 * @param labelSpacing The spacing for item labels.
 * @param axisSpacing The spacing for the numeric axis.
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
    barSpacing: Dp = SPACING_MEDIUM,
    cornerRadius: Dp = SPACING_SMALL,
    showValuesInside: Boolean = false,
    labelSpacing: Dp = CHART_LABEL_SPACING,
    axisSpacing: Dp = CHART_AXIS_SPACING,
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
        val txtPaddingPx = SPACING_SMALL.toPx()
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
                        strokeWidth = BORDER_WIDTH_NORMAL.toPx(),
                    )

                    val measuredValue = textMeasurer.measure(value.toString(), textStyle)
                    drawText(
                        textMeasurer = textMeasurer,
                        text = value.toString(),
                        style = textStyle.copy(color = textColor.copy(alpha = 0.8f)),
                        topLeft = Offset(axisPaddingPx - measuredValue.size.width - (2 * txtPaddingPx), y - measuredValue.size.height / 2f),
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

                // Draw value inside
                if (showValuesInside) {
                    val valueText = value.roundToInt().toString()
                    val measuredValue = textMeasurer.measure(valueText, textStyle)
                    if (barHeight > measuredValue.size.height + txtPaddingPx) {
                        drawText(
                            textMeasurer = textMeasurer,
                            text = valueText,
                            style = textStyle.copy(color = Color.White), // Fixed white for contrast on primary bars
                            topLeft = Offset(
                                left + (barWidth - measuredValue.size.width) / 2,
                                chartHeight - barHeight + txtPaddingPx,
                            ),
                        )
                    }
                }

                labels?.getOrNull(index)?.let { label ->
                    val measuredText = textMeasurer.measure(label, textStyle)
                    drawText(
                        textMeasurer = textMeasurer,
                        text = label,
                        style = textStyle.copy(color = textColor),
                        topLeft = Offset(
                            left + (barWidth - measuredText.size.width) / 2,
                            chartHeight + txtPaddingPx,
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
                        strokeWidth = BORDER_WIDTH_NORMAL.toPx(),
                    )

                    val measuredValue = textMeasurer.measure(value.toString(), textStyle)
                    drawText(
                        textMeasurer = textMeasurer,
                        text = value.toString(),
                        style = textStyle.copy(color = textColor.copy(alpha = 0.8f)),
                        topLeft = Offset(x - measuredValue.size.width / 2f, chartHeight + txtPaddingPx),
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

                // Draw value inside
                if (showValuesInside) {
                    val valueText = value.roundToInt().toString()
                    val measuredValue = textMeasurer.measure(valueText, textStyle)
                    if (barWidth > measuredValue.size.width + (2 * txtPaddingPx)) {
                        drawText(
                            textMeasurer = textMeasurer,
                            text = valueText,
                            style = textStyle.copy(color = Color.White),
                            topLeft = Offset(
                                labelPaddingPx + barWidth - measuredValue.size.width - txtPaddingPx,
                                top + (barHeight - measuredValue.size.height) / 2,
                            ),
                        )
                    }
                }

                labels?.getOrNull(index)?.let { label ->
                    val measuredText = textMeasurer.measure(label, textStyle)
                    drawText(
                        textMeasurer = textMeasurer,
                        text = label,
                        style = textStyle.copy(color = textColor),
                        topLeft = Offset(
                            labelPaddingPx - measuredText.size.width - txtPaddingPx,
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
    Column(Modifier.padding(SPACING_LARGE)) {
        MuzzicBarChart(
            data = listOf(10f, 50f, 30f, 80f, 20f),
            colors = listOf(Color.Magenta),
            labels = listOf("Jan", "Feb", "Mar", "Apr", "May"),
            showAxis = true,
            modifier = Modifier.size(CHART_PREVIEW_WIDTH_LARGE, CHART_PREVIEW_HEIGHT_LARGE),
            orientation = BarChartOrientation.Vertical,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun MuzzicBarChartSharpPreview() {
    Column(Modifier.padding(SPACING_LARGE)) {
        MuzzicBarChart(
            data = listOf(40f, 80f, 60f),
            colors = listOf(Color.Blue),
            cornerRadius = SPACING_TINY, // 0.dp might be better as 0.dp but let's see. 0.dp is fine.
            modifier = Modifier.size(CHART_PREVIEW_WIDTH_MEDIUM, CHART_PREVIEW_HEIGHT_SMALL),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun MuzzicBarChartRoundedPreview() {
    Column(Modifier.padding(SPACING_LARGE)) {
        MuzzicBarChart(
            data = listOf(40f, 80f, 60f),
            colors = listOf(Color.Green),
            cornerRadius = CHART_FULL_ROUNDING,
            modifier = Modifier.size(CHART_PREVIEW_WIDTH_MEDIUM, CHART_PREVIEW_HEIGHT_SMALL),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun MuzzicBarChartHorizontalWithLabelsPreview() {
    Column(Modifier.padding(SPACING_LARGE)) {
        MuzzicBarChart(
            data = listOf(10f, 50f, 30f, 80f, 20f),
            colors = listOf(Color.Cyan),
            labels = listOf("A", "B", "C", "D", "E"),
            showAxis = true,
            modifier = Modifier.size(CHART_PREVIEW_WIDTH_LARGE, CHART_PREVIEW_HEIGHT_LARGE),
            orientation = BarChartOrientation.Horizontal,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun MuzzicBarChartVerticalWithValuesInsidePreview() {
    Column(Modifier.padding(SPACING_LARGE)) {
        MuzzicBarChart(
            data = listOf(10f, 50f, 30f, 80f, 20f),
            colors = listOf(Color.Blue),
            showValuesInside = true,
            modifier = Modifier.size(CHART_PREVIEW_WIDTH_LARGE, CHART_PREVIEW_HEIGHT_LARGE),
            orientation = BarChartOrientation.Vertical,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun MuzzicBarChartHorizontalWithValuesInsidePreview() {
    Column(Modifier.padding(SPACING_LARGE)) {
        MuzzicBarChart(
            data = listOf(10f, 50f, 30f, 80f, 20f),
            colors = listOf(Color.Green),
            showValuesInside = true,
            showAxis = true,
            modifier = Modifier.size(CHART_PREVIEW_WIDTH_LARGE, CHART_PREVIEW_HEIGHT_LARGE),
            orientation = BarChartOrientation.Horizontal,
        )
    }
}
