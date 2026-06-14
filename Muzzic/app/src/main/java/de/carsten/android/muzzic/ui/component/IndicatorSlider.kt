package de.carsten.android.muzzic.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
 * @param indicatorAlignment The alignment of the indicator relative to the slider.
 * @param indicatorOffsetY The vertical offset for the indicator.
 */
@Composable
fun IndicatorSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    colors: SliderColors = SliderDefaults.colors(),
    indicatorFormatter: (Float) -> String,
    indicatorColor: Color = MaterialTheme.colorScheme.primary,
    indicatorAlignment: Alignment = Alignment.TopCenter,
    indicatorOffsetY: Dp = (-40).dp,
) {
    var scrubbingProgress by remember { mutableFloatStateOf(-1f) }
    val displayValue = if (scrubbingProgress >= 0f) scrubbingProgress else value
    val density = LocalDensity.current

    Box(contentAlignment = indicatorAlignment) {
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
            valueRange = valueRange,
            colors = colors,
        )

        if (scrubbingProgress >= 0f) {
            Surface(
                modifier = Modifier.offset {
                    IntOffset(0, with(density) { indicatorOffsetY.roundToPx() })
                },
                color = indicatorColor,
                shape = RoundedCornerShape(4.dp),
            ) {
                Text(
                    text = indicatorFormatter(scrubbingProgress),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}
