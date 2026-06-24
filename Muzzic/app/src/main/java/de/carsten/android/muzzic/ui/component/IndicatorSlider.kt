package de.carsten.android.muzzic.ui.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import de.carsten.android.muzzic.ui.FONT_SIZE_CAPTION
import de.carsten.android.muzzic.ui.INDICATOR_SLIDER_OFFSET_Y
import de.carsten.android.muzzic.ui.INDICATOR_SLIDER_PADDING_TOP_WITH_TAIL
import de.carsten.android.muzzic.ui.INDICATOR_SLIDER_PADDING_WITH_TAIL
import de.carsten.android.muzzic.ui.INDICATOR_SLIDER_PREVIEW_PADDING
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.SPACING_MEDIUM
import de.carsten.android.muzzic.ui.SPACING_SMALL
import de.carsten.android.muzzic.ui.shape.IndicatorShape
import de.carsten.android.muzzic.ui.shape.TailDirection
import de.carsten.android.muzzic.ui.theme.AppTheme

/**
 * A generic slider component that displays a numeric indicator while the user is scrubbing.
 *
 * @param value The current value of the slider.
 * @param onValueChange Callback for when the value changes.
 * @param onValueChangeFinished Callback for when the user stops scrubbing. Passes the final value.
 * @param modifier The modifier to be applied to the slider.
 * @param valueRange The range of values the slider can represent.
 * @param colors The colors to be used for the slider.
 * @param indicatorFormatter A function that converts the current float value to a string for display in the indicator.
 * @param indicatorColor The background color of the indicator bubble.
 * @param indicatorAlignment The alignment of the indicator relative to the thumb.
 * @param indicatorOffsetY The vertical offset for the indicator.
 * @param indicatorRotation The rotation of the indicator content. Useful when the slider itself is rotated.
 * @param tailDirection The direction the indicator tail points to.
 * @param enabled whether or not the slider is enabled.
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s for this slider.
 * @param initialScrubbingProgress The initial scrubbing progress to display in the indicator. Set to -1f to hide the indicator initially. Useful for previews.
 */
@Composable
fun IndicatorSlider(
    value: Float,
    onValueChange: (Float) -> Unit = {},
    onValueChangeFinished: (Float) -> Unit = {},
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    colors: SliderColors = SliderDefaults.colors(),
    indicatorFormatter: (Float) -> String = { f -> f.toString() },
    indicatorColor: Color = MaterialTheme.colorScheme.primary,
    indicatorAlignment: Alignment = Alignment.TopCenter,
    indicatorOffsetY: Dp = INDICATOR_SLIDER_OFFSET_Y,
    indicatorRotation: Float = 0f,
    tailDirection: TailDirection = TailDirection.Bottom,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    initialScrubbingProgress: Float = -1f,
) {
    IndicatorSliderContent(
        value = value,
        initialScrubbingProgress = initialScrubbingProgress,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        modifier = modifier,
        valueRange = valueRange,
        colors = colors,
        indicatorFormatter = indicatorFormatter,
        indicatorColor = indicatorColor,
        indicatorAlignment = indicatorAlignment,
        indicatorOffsetY = indicatorOffsetY,
        indicatorRotation = indicatorRotation,
        tailDirection = tailDirection,
        enabled = enabled,
        interactionSource = interactionSource,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IndicatorSliderContent(
    value: Float,
    initialScrubbingProgress: Float = -1f,
    onValueChange: (Float) -> Unit = {},
    onValueChangeFinished: (Float) -> Unit = {},
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    colors: SliderColors = SliderDefaults.colors(),
    indicatorFormatter: (Float) -> String = { f -> f.toString() },
    indicatorColor: Color = MaterialTheme.colorScheme.primary,
    indicatorAlignment: Alignment = Alignment.TopCenter,
    indicatorOffsetY: Dp = INDICATOR_SLIDER_OFFSET_Y,
    indicatorRotation: Float = 0f,
    tailDirection: TailDirection = TailDirection.Bottom,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    var scrubbingProgress by remember { mutableFloatStateOf(initialScrubbingProgress) }
    val displayValue = if (scrubbingProgress >= 0f) scrubbingProgress else value
    val density = LocalDensity.current
    val indicatorShape = remember(tailDirection) { IndicatorShape(direction = tailDirection) }

    Slider(
        value = displayValue,
        onValueChange = {
            scrubbingProgress = it
            onValueChange(it)
        },
        onValueChangeFinished = {
            onValueChangeFinished(scrubbingProgress)
            scrubbingProgress = -1f
        },
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        colors = colors,
        interactionSource = interactionSource,
        thumb = {
            Box(
                modifier = Modifier.graphicsLayer { clip = false },
                contentAlignment = Alignment.Center,
            ) {
                SliderDefaults.Thumb(
                    interactionSource = interactionSource,
                    colors = colors,
                    enabled = enabled,
                )

                if (scrubbingProgress >= 0f) {
                    val isPreview = LocalInspectionMode.current
                    if (isPreview) {
                        Surface(
                            modifier = Modifier
                                .align(indicatorAlignment)
                                .offset(y = indicatorOffsetY)
                                .graphicsLayer { rotationZ = indicatorRotation },
                            color = indicatorColor,
                            shape = indicatorShape,
                        ) {
                            Text(
                                text = indicatorFormatter(scrubbingProgress),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = FONT_SIZE_CAPTION,
                                modifier = Modifier.padding(
                                    start = if (tailDirection == TailDirection.Left) INDICATOR_SLIDER_PADDING_WITH_TAIL else SPACING_MEDIUM,
                                    end = if (tailDirection == TailDirection.Right) INDICATOR_SLIDER_PADDING_WITH_TAIL else SPACING_MEDIUM,
                                    top = if (tailDirection == TailDirection.Top) INDICATOR_SLIDER_PADDING_TOP_WITH_TAIL else SPACING_SMALL,
                                    bottom = if (tailDirection == TailDirection.Bottom) INDICATOR_SLIDER_PADDING_TOP_WITH_TAIL else SPACING_SMALL,
                                ),
                            )
                        }
                    } else {
                        Popup(
                            alignment = indicatorAlignment,
                            offset = IntOffset(0, with(density) { indicatorOffsetY.roundToPx() }),
                            properties = PopupProperties(clippingEnabled = false),
                        ) {
                            Surface(
                                modifier = Modifier.graphicsLayer { rotationZ = indicatorRotation },
                                color = indicatorColor,
                                shape = indicatorShape,
                            ) {
                                Text(
                                    text = indicatorFormatter(scrubbingProgress),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = FONT_SIZE_CAPTION,
                                    modifier = Modifier.padding(
                                        start = if (tailDirection == TailDirection.Left) INDICATOR_SLIDER_PADDING_WITH_TAIL else SPACING_MEDIUM,
                                        end = if (tailDirection == TailDirection.Right) INDICATOR_SLIDER_PADDING_WITH_TAIL else SPACING_MEDIUM,
                                        top = if (tailDirection == TailDirection.Top) INDICATOR_SLIDER_PADDING_TOP_WITH_TAIL else SPACING_SMALL,
                                        bottom = if (tailDirection == TailDirection.Bottom) INDICATOR_SLIDER_PADDING_TOP_WITH_TAIL else SPACING_SMALL,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        },
    )
}

@Composable
@Preview
@Preview(uiMode = PREVIEW_DARK_MODE)
fun IndicatorSliderPreview() {
    AppTheme {
        Box(modifier = Modifier.padding(top = INDICATOR_SLIDER_PREVIEW_PADDING)) {
            IndicatorSliderContent(
                value = 0.5f,
                initialScrubbingProgress = 0.6f,
            )
        }
    }
}
