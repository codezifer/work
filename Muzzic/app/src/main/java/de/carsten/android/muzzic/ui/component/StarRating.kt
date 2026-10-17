package de.carsten.android.muzzic.ui.component

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import de.carsten.android.muzzic.MAX_STARS
import de.carsten.android.muzzic.R
import de.carsten.android.muzzic.ui.ICON_SIZE_MEDIUM
import de.carsten.android.muzzic.ui.PREVIEW_DARK_MODE
import de.carsten.android.muzzic.ui.STAR_RATING_ICON_SPACING
import de.carsten.android.muzzic.ui.theme.AppTheme
import de.carsten.android.muzzic.ui.theme.CustomColors
import kotlin.math.roundToInt

/**
 * Number of half-star steps the full scale is divided into (5 stars * 2).
 */
private const val HALF_STEPS_PER_STAR = 2

/**
 * Displays a 0..5 star rating in 0.5 steps and, when [editable], lets the user
 * change it by tapping or horizontally dragging across the stars.
 *
 * A tap on the currently set value clears the rating (= 0) via [onRatingCleared].
 *
 * @param rating current rating in the range 0f..5f, quantized to 0.5 steps.
 * @param modifier optional [Modifier] for the star row.
 * @param starSize size of a single star icon.
 * @param editable when `true`, tap and horizontal drag gestures update the rating.
 * @param onRatingChanged invoked with the new rating (0f..5f) while editing.
 * @param onRatingCleared invoked when the user taps the already set value.
 */
@Composable
fun StarRating(
    rating: Float,
    modifier: Modifier = Modifier,
    starSize: Dp = ICON_SIZE_MEDIUM,
    editable: Boolean = false,
    onRatingChanged: (Float) -> Unit = {},
    onRatingCleared: () -> Unit = {},
) {
    val starColor = CustomColors.starColor
    val emptyColor = MaterialTheme.colorScheme.outline
    val ratingDescription = stringResource(R.string.rating_value_format, rating)

    Row(
        modifier = modifier
            .semantics { contentDescription = ratingDescription }
            .then(
                if (editable) {
                    Modifier
                        // Tapping a position sets the value; tapping the value already
                        // set clears the rating.
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val value = ratingValueAt(offset.x, size.width)
                                if (value == rating && value > 0f) {
                                    onRatingCleared()
                                } else {
                                    onRatingChanged(value)
                                }
                            }
                        }
                        // Dragging fills or lowers the stars up to the touched position.
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { change, _ ->
                                onRatingChanged(ratingValueAt(change.position.x, size.width))
                            }
                        }
                } else {
                    Modifier
                },
            ),
        horizontalArrangement = Arrangement.spacedBy(STAR_RATING_ICON_SPACING),
    ) {
        repeat(MAX_STARS) { index ->
            val fill = rating - index
            val icon = when {
                fill >= 1f -> Icons.Filled.Star
                fill >= 0.5f -> Icons.AutoMirrored.Filled.StarHalf
                else -> Icons.Filled.StarBorder
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (fill >= 0.5f) starColor else emptyColor,
                modifier = Modifier.size(starSize),
            )
        }
    }
}

/**
 * Maps a horizontal touch position to the nearest 0.5 star step.
 *
 * @param x horizontal position in pixels relative to the star row.
 * @param width width of the star row in pixels.
 * @return rating in the range 0f..5f, quantized to 0.5 steps.
 */
private fun ratingValueAt(x: Float, width: Int): Float {
    if (width <= 0) return 0f
    val fraction = (x / width).coerceIn(0f, 1f)
    val step = (fraction * MAX_STARS * HALF_STEPS_PER_STAR).roundToInt()
    return step / HALF_STEPS_PER_STAR.toFloat()
}

@Composable
@Preview(showBackground = true, name = "Read-only Light")
@Preview(uiMode = PREVIEW_DARK_MODE, showBackground = true, name = "Read-only Dark")
fun StarRatingPreview() {
    AppTheme {
        StarRating(rating = 3.5f)
    }
}

@Composable
@Preview(showBackground = true, name = "Editable Light")
@Preview(uiMode = PREVIEW_DARK_MODE, showBackground = true, name = "Editable Dark")
fun StarRatingEditablePreview() {
    AppTheme {
        StarRating(rating = 2.5f, editable = true)
    }
}
