package de.carsten.android.muzzic.ui.component.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import de.carsten.android.muzzic.ui.BORDER_WIDTH_NORMAL
import de.carsten.android.muzzic.ui.CHART_AXIS_SPACING
import de.carsten.android.muzzic.ui.CHART_FULL_ROUNDING
import de.carsten.android.muzzic.ui.CHART_LABEL_AREA_MAX_HEIGHT_RATIO
import de.carsten.android.muzzic.ui.CHART_LABEL_MIN_WIDTH
import de.carsten.android.muzzic.ui.CHART_LABEL_ROTATION_DIAGONAL
import de.carsten.android.muzzic.ui.CHART_LABEL_ROTATION_NONE
import de.carsten.android.muzzic.ui.CHART_LABEL_ROTATION_VERTICAL
import de.carsten.android.muzzic.ui.CHART_LABEL_SPACING
import de.carsten.android.muzzic.ui.CHART_PREVIEW_HEIGHT_LARGE
import de.carsten.android.muzzic.ui.CHART_PREVIEW_HEIGHT_SMALL
import de.carsten.android.muzzic.ui.CHART_PREVIEW_WIDTH_LARGE
import de.carsten.android.muzzic.ui.CHART_PREVIEW_WIDTH_MEDIUM
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.SPACING_LARGE
import de.carsten.android.muzzic.ui.SPACING_MEDIUM
import de.carsten.android.muzzic.ui.SPACING_SMALL
import de.carsten.android.muzzic.ui.SPACING_TINY
import de.carsten.android.muzzic.ui.theme.AppTheme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

enum class BarChartOrientation { Vertical, Horizontal }

/**
 * Below this cosine/sine magnitude a rotation axis is treated as perpendicular to the reading
 * direction, which avoids dividing by zero for multiples of 90°.
 */
private const val ROTATION_AXIS_EPSILON = 1e-3f

/**
 * Axis-aligned bounds of a text box after rotating it by [degrees] around its centre.
 *
 * A rotated box needs more room than the unrotated one, which is why the label area of the
 * chart is derived from these bounds instead of the plain text size.
 */
private fun rotatedLabelSize(width: Float, height: Float, degrees: Float): Size {
    val radians = degrees * PI / 180.0
    val absCos = abs(cos(radians))
    val absSin = abs(sin(radians))
    return Size(
        width = (width * absCos + height * absSin).toFloat(),
        height = (width * absSin + height * absCos).toFloat(),
    )
}

/**
 * Largest text width whose label box still fits the given slot after rotating it by [degrees].
 *
 * Inverse of [rotatedLabelSize] applied to both axes: [slotWidthPx] bounds the horizontal extent,
 * [availableHeightPx] the vertical one. An axis that cannot be constrained because of the rotation
 * (e.g. the vertical extent of a 0° label) is ignored.
 *
 * @param slotWidthPx Horizontal room the label may occupy before it would overlap its neighbour.
 * @param availableHeightPx Vertical room the label may occupy below the axis.
 * @param lineHeightPx Height of a single line of [textStyle]; grows the box but cannot be truncated.
 * @param degrees Rotation applied to the label, matching [labelRotationDegrees].
 * @param minWidthPx Floor that keeps a truncated label identifiable.
 * @return Maximum text width in pixels for the given slot.
 */
private fun maxLabelTextWidthPx(slotWidthPx: Float, availableHeightPx: Float, lineHeightPx: Float, degrees: Float, minWidthPx: Float): Float {
    val radians = degrees * PI / 180.0
    val absCos = abs(cos(radians)).toFloat()
    val absSin = abs(sin(radians)).toFloat()
    val byWidth = if (absCos > ROTATION_AXIS_EPSILON) {
        (slotWidthPx - lineHeightPx * absSin) / absCos
    } else {
        Float.POSITIVE_INFINITY
    }
    val byHeight = if (absSin > ROTATION_AXIS_EPSILON) {
        (availableHeightPx - lineHeightPx * absCos) / absSin
    } else {
        Float.POSITIVE_INFINITY
    }
    return minOf(byWidth, byHeight).coerceAtLeast(minWidthPx)
}

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
 * @param labelRotationDegrees Rotation of the item labels in degrees. Only applies to
 *   [BarChartOrientation.Vertical]; negative values (e.g. [CHART_LABEL_ROTATION_DIAGONAL] or
 *   [CHART_LABEL_ROTATION_VERTICAL]) tilt the labels so they read from bottom to top. When the
 *   rotated labels would overlap horizontally, single labels are skipped automatically.
 * @param labelAreaSize Space reserved for the item labels. `null` derives the size from the tallest
 *   rotated label (capped by [CHART_LABEL_AREA_MAX_HEIGHT_RATIO]); for
 *   [BarChartOrientation.Horizontal] it describes the width of the label column and defaults to
 *   [CHART_LABEL_SPACING].
 * @param labelMaxWidth Upper bound for the width of a single item label before it is truncated with
 *   an ellipsis. `null` derives the bound from the layout: the slot width (bar width for
 *   [BarChartOrientation.Vertical], label column width for [BarChartOrientation.Horizontal]) and,
 *   for rotated labels, the reserved label area. Rotation is ignored for
 *   [BarChartOrientation.Horizontal].
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
    labelRotationDegrees: Float = CHART_LABEL_ROTATION_NONE,
    labelAreaSize: Dp? = null,
    labelMaxWidth: Dp? = null,
    axisSpacing: Dp = CHART_AXIS_SPACING,
    textStyle: TextStyle = MaterialTheme.typography.labelSmall,
) {
    if (data.isEmpty()) return
    val maxVal = data.maxOrNull() ?: 0f
    if (maxVal == 0f) return

    val textMeasurer = rememberTextMeasurer()
    val textColor = textStyle.color.takeIf { it != Color.Unspecified } ?: MaterialTheme.colorScheme.onSurface

    // Measuring once outside the draw scope avoids re-measuring every label on each frame.
    val labelLayouts = remember(labels, textStyle, textMeasurer) {
        labels?.map { textMeasurer.measure(text = it, style = textStyle) }.orEmpty()
    }

    Canvas(modifier = modifier) {
        val spacingPx = barSpacing.toPx()
        val cornerRadiusPx = cornerRadius.toPx()

        // Padding for labels and axis
        val txtPaddingPx = SPACING_SMALL.toPx()
        val labelPaddingPx = labelAreaSize?.toPx() ?: CHART_LABEL_SPACING.toPx()
        val axisPaddingPx = if (showAxis) axisSpacing.toPx() else 0f

        /**
         * Pass 2: truncates every label that does not fit its slot. Labels that already fit keep
         * their pass-1 layout, so the measurer cache is only stressed by the genuinely long ones.
         *
         * @param slotWidthPx Horizontal room one label may occupy before it oversteps its slot.
         * @param availableHeightPx Vertical room the label may occupy below the axis.
         * @param degrees Rotation applied to the label when measuring its footprint.
         * @return One layout per label, shortened with an ellipsis where the slot is too small.
         */
        fun fitLabels(slotWidthPx: Float, availableHeightPx: Float, degrees: Float): List<TextLayoutResult> {
            val rawLabels = labels ?: return emptyList()
            val maxTextWidthPx = labelMaxWidth?.toPx() ?: maxLabelTextWidthPx(
                slotWidthPx = slotWidthPx,
                availableHeightPx = availableHeightPx,
                lineHeightPx = labelLayouts.firstOrNull()?.size?.height?.toFloat() ?: 0f,
                degrees = degrees,
                minWidthPx = CHART_LABEL_MIN_WIDTH.toPx(),
            )
            val maxTextWidthIntPx = ceil(maxTextWidthPx).toInt()
            return labelLayouts.mapIndexed { index, unbounded ->
                if (unbounded.size.width <= maxTextWidthIntPx) {
                    unbounded
                } else {
                    textMeasurer.measure(
                        text = rawLabels[index],
                        style = textStyle,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        constraints = Constraints(maxWidth = maxTextWidthIntPx),
                    )
                }
            }
        }

        if (orientation == BarChartOrientation.Vertical) {
            val chartWidth = size.width - axisPaddingPx
            val barWidth = (chartWidth - (spacingPx * (data.size - 1))) / data.size

            // Pass 1 defines how much room the labels get; that area in turn bounds pass 2, which
            // keeps the two passes from depending on each other.
            val tallestLabelPx = labelLayouts.maxOfOrNull { layout ->
                rotatedLabelSize(
                    width = layout.size.width.toFloat(),
                    height = layout.size.height.toFloat(),
                    degrees = labelRotationDegrees,
                ).height
            } ?: 0f
            val autoLabelAreaPx = (tallestLabelPx + txtPaddingPx)
                .coerceAtMost(size.height * CHART_LABEL_AREA_MAX_HEIGHT_RATIO)
            val labelAreaPx = (labelAreaSize?.toPx() ?: autoLabelAreaPx).coerceIn(0f, size.height)
            val chartHeight = size.height - labelAreaPx

            // Pass 2 truncates once for both rotation cases: at 0° only the slot width binds, at
            // ±45°/±90° the reserved label area does.
            val fittedLabelLayouts = fitLabels(
                slotWidthPx = barWidth,
                availableHeightPx = (labelAreaPx - txtPaddingPx).coerceAtLeast(0f),
                degrees = labelRotationDegrees,
            )

            // Rotated labels need more horizontal room, so labels that would collide are skipped.
            val visibleLabelIndices: Iterable<Int> = if (labelRotationDegrees == CHART_LABEL_ROTATION_NONE) {
                fittedLabelLayouts.indices
            } else {
                val visible = mutableListOf<Int>()
                var rightEdgeOfPrevious = Float.NEGATIVE_INFINITY
                fittedLabelLayouts.indices.forEach { index ->
                    val box = fittedLabelLayouts[index].size
                    val boxWidth = rotatedLabelSize(box.width.toFloat(), box.height.toFloat(), labelRotationDegrees).width
                    val centerX = axisPaddingPx + index * (barWidth + spacingPx) + barWidth / 2f
                    if (centerX - boxWidth / 2f >= rightEdgeOfPrevious + txtPaddingPx) {
                        visible += index
                        rightEdgeOfPrevious = centerX + boxWidth / 2f
                    }
                }
                visible
            }

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

            // Draw bars
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
            }

            // Item labels rotate around their own centre, so the rotated box stays below the axis.
            visibleLabelIndices.forEach { index ->
                val layout = fittedLabelLayouts[index]
                val labelWidth = layout.size.width.toFloat()
                val labelHeight = layout.size.height.toFloat()
                val rotatedBox = rotatedLabelSize(labelWidth, labelHeight, labelRotationDegrees)
                val center = Offset(
                    x = axisPaddingPx + index * (barWidth + spacingPx) + barWidth / 2f,
                    y = chartHeight + txtPaddingPx + rotatedBox.height / 2f,
                )
                rotate(degrees = labelRotationDegrees, pivot = center) {
                    drawText(
                        textLayoutResult = layout,
                        color = textColor,
                        topLeft = center - Offset(labelWidth / 2f, labelHeight / 2f),
                    )
                }
            }
        } else {
            val chartHeight = size.height - axisPaddingPx
            val barHeight = (chartHeight - (spacingPx * (data.size - 1))) / data.size

            // Rotation is ignored for horizontal bars, so only the label column width constrains.
            val fittedLabelLayouts = fitLabels(
                slotWidthPx = (labelPaddingPx - txtPaddingPx).coerceAtLeast(0f),
                availableHeightPx = barHeight,
                degrees = CHART_LABEL_ROTATION_NONE,
            )

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

                // Rotation is intentionally ignored for horizontal bars: the labels sit in the
                // narrow row of their bar, so tilting them would break the row alignment.
                fittedLabelLayouts.getOrNull(index)?.let { layout ->
                    drawText(
                        textLayoutResult = layout,
                        color = textColor,
                        topLeft = Offset(
                            labelPaddingPx - layout.size.width - txtPaddingPx,
                            top + (barHeight - layout.size.height) / 2f,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * Preview host: applies [AppTheme] so the canvas text picks up the theme's `onSurface` colour
 * instead of the light baseline colour scheme.
 */
@Composable
private fun ChartPreview(content: @Composable () -> Unit) {
    AppTheme {
        Column(Modifier.padding(SPACING_LARGE)) {
            content()
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, name = "Light Mode")
@Preview(uiMode = PREVIEW_DARK_MODE, showBackground = true, backgroundColor = 0xFF111418, name = "Dark Mode")
@Composable
fun MuzzicBarChartVerticalWithLabelsPreview() {
    ChartPreview {
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

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, name = "Diagonal Light Mode")
@Preview(uiMode = PREVIEW_DARK_MODE, showBackground = true, backgroundColor = 0xFF111418, name = "Diagonal Dark Mode")
@Composable
fun MuzzicBarChartDiagonalLabelsPreview() {
    ChartPreview {
        MuzzicBarChart(
            data = listOf(10f, 50f, 30f, 80f, 20f, 45f),
            colors = listOf(Color.Magenta),
            labels = listOf("2026-01", "2026-02", "2026-03", "2026-04", "2026-05", "2026-06"),
            showAxis = true,
            labelRotationDegrees = CHART_LABEL_ROTATION_DIAGONAL,
            modifier = Modifier.size(CHART_PREVIEW_WIDTH_LARGE, CHART_PREVIEW_HEIGHT_LARGE),
            orientation = BarChartOrientation.Vertical,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, name = "Vertical Light Mode")
@Preview(uiMode = PREVIEW_DARK_MODE, showBackground = true, backgroundColor = 0xFF111418, name = "Vertical Dark Mode")
@Composable
fun MuzzicBarChartVerticalLabelsPreview() {
    ChartPreview {
        MuzzicBarChart(
            data = listOf(10f, 50f, 30f, 80f, 20f, 45f),
            colors = listOf(Color.Magenta),
            labels = listOf("2026-01", "2026-02", "2026-03", "2026-04", "2026-05", "2026-06"),
            showAxis = true,
            labelRotationDegrees = CHART_LABEL_ROTATION_VERTICAL,
            modifier = Modifier.size(CHART_PREVIEW_WIDTH_LARGE, CHART_PREVIEW_HEIGHT_LARGE),
            orientation = BarChartOrientation.Vertical,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, name = "Long Labels Light Mode")
@Preview(uiMode = PREVIEW_DARK_MODE, showBackground = true, backgroundColor = 0xFF111418, name = "Long Labels Dark Mode")
@Composable
fun MuzzicBarChartLongLabelTruncationPreview() {
    ChartPreview {
        MuzzicBarChart(
            data = listOf(10f, 7f, 5f, 4f, 9f),
            colors = listOf(Color.Magenta),
            labels = listOf("Dimmu Borgir", "Cradle Of Filth", "Interpol", "Chvrches", "Wolfclub"),
            showValuesInside = true,
            labelRotationDegrees = CHART_LABEL_ROTATION_VERTICAL,
            modifier = Modifier.size(CHART_PREVIEW_WIDTH_MEDIUM, CHART_PREVIEW_HEIGHT_SMALL),
            orientation = BarChartOrientation.Vertical,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun MuzzicBarChartSharpPreview() {
    ChartPreview {
        MuzzicBarChart(
            data = listOf(40f, 80f, 60f),
            colors = listOf(Color.Blue),
            cornerRadius = SPACING_TINY,
            modifier = Modifier.size(CHART_PREVIEW_WIDTH_MEDIUM, CHART_PREVIEW_HEIGHT_SMALL),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun MuzzicBarChartRoundedPreview() {
    ChartPreview {
        MuzzicBarChart(
            data = listOf(40f, 80f, 60f),
            colors = listOf(Color.Green),
            cornerRadius = CHART_FULL_ROUNDING,
            modifier = Modifier.size(CHART_PREVIEW_WIDTH_MEDIUM, CHART_PREVIEW_HEIGHT_SMALL),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, name = "Horizontal Light Mode")
@Preview(uiMode = PREVIEW_DARK_MODE, showBackground = true, backgroundColor = 0xFF111418, name = "Horizontal Dark Mode")
@Composable
fun MuzzicBarChartHorizontalWithLabelsPreview() {
    ChartPreview {
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
    ChartPreview {
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
    ChartPreview {
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
