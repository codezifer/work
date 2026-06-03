package de.carsten.android.muzzic.ui.theme

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private const val LIST_ITEM_HEIGHT_DP = 56f
private const val LIST_ITEM_CORNER_RADIUS_DP = 8f

/**
 * The proportional corner size for list item leading elements.
 * Calculated as (8dp / 56dp) * 100 ≈ 14.28%
 *
 * Proportional rounding ensures that the corner radius scales with the component size,
 * maintaining design consistency across different screen densities and element scales.
 */
const val LIST_ITEM_LEADING_CORNER_PERCENT = (LIST_ITEM_CORNER_RADIUS_DP / LIST_ITEM_HEIGHT_DP) * 100f

/**
 * A shape designed for leading images in list items that are flush with the card's edge.
 * It provides a proportional rounding on the start side based on [LIST_ITEM_LEADING_CORNER_PERCENT].
 */
val ListItemLeadingShape = RoundedCornerShape(
    topStart = CornerSize(LIST_ITEM_LEADING_CORNER_PERCENT),
    bottomStart = CornerSize(LIST_ITEM_LEADING_CORNER_PERCENT),
    topEnd = CornerSize(0.dp),
    bottomEnd = CornerSize(0.dp),
)
