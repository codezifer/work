package de.carsten.android.muzzic.ui.shape

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Direction where the tail of the indicator bubble points to.
 */
enum class TailDirection {
    Top,
    Bottom,
    Left,
    Right,
}

/**
 * A custom shape for the indicator bubble that includes a triangular tail.
 *
 * Body and tail are unioned into one contour; [tailOverlap] sinks the tail base into the body.
 */
class IndicatorShape(
    private val tailSize: Dp = 6.dp,
    private val cornerRadius: Dp = 6.dp,
    private val direction: TailDirection = TailDirection.Bottom,
    private val tailOverlap: Dp = 0.dp,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val body = Path()
        val tail = Path()
        val tailPx = with(density) { tailSize.toPx() }
        val radiusPx = with(density) { cornerRadius.toPx() }
        val overlapPx = with(density) { tailOverlap.toPx() }
        val halfWidth = size.width / 2
        val halfHeight = size.height / 2

        when (direction) {
            TailDirection.Bottom -> {
                val rectHeight = size.height - tailPx
                body.addRoundRect(RoundRect(0f, 0f, size.width, rectHeight, CornerRadius(radiusPx)))
                tail.moveTo(halfWidth - tailPx, rectHeight - overlapPx)
                tail.lineTo(halfWidth, size.height)
                tail.lineTo(halfWidth + tailPx, rectHeight - overlapPx)
            }

            TailDirection.Top -> {
                body.addRoundRect(RoundRect(0f, tailPx, size.width, size.height, CornerRadius(radiusPx)))
                tail.moveTo(halfWidth - tailPx, tailPx + overlapPx)
                tail.lineTo(halfWidth, 0f)
                tail.lineTo(halfWidth + tailPx, tailPx + overlapPx)
            }

            TailDirection.Left -> {
                body.addRoundRect(RoundRect(tailPx, 0f, size.width, size.height, CornerRadius(radiusPx)))
                tail.moveTo(tailPx + overlapPx, halfHeight - tailPx)
                tail.lineTo(0f, halfHeight)
                tail.lineTo(tailPx + overlapPx, halfHeight + tailPx)
            }

            TailDirection.Right -> {
                val rectWidth = size.width - tailPx
                body.addRoundRect(RoundRect(0f, 0f, rectWidth, size.height, CornerRadius(radiusPx)))
                tail.moveTo(rectWidth - overlapPx, halfHeight - tailPx)
                tail.lineTo(size.width, halfHeight)
                tail.lineTo(rectWidth - overlapPx, halfHeight + tailPx)
            }
        }
        tail.close()

        // Union keeps body and tail one contour, so no seam shows between them
        val merged = Path()
        merged.op(body, tail, PathOperation.Union)
        return Outline.Generic(merged)
    }
}
