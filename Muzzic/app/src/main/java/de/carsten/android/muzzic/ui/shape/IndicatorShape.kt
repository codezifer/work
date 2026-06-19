package de.carsten.android.muzzic.ui.shape

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
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
 */
class IndicatorShape(private val tailSize: Dp = 6.dp, private val cornerRadius: Dp = 6.dp, private val direction: TailDirection = TailDirection.Bottom) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val tailPx = with(density) { tailSize.toPx() }
        val radiusPx = with(density) { cornerRadius.toPx() }

        when (direction) {
            TailDirection.Bottom -> {
                val rectHeight = size.height - tailPx
                path.addRoundRect(
                    RoundRect(
                        0f,
                        0f,
                        size.width,
                        rectHeight,
                        CornerRadius(radiusPx),
                    ),
                )
                path.moveTo(size.width / 2 - tailPx, rectHeight)
                path.lineTo(size.width / 2, size.height)
                path.lineTo(size.width / 2 + tailPx, rectHeight)
            }

            TailDirection.Top -> {
                path.addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        0f,
                        tailPx,
                        size.width,
                        size.height,
                        CornerRadius(radiusPx),
                    ),
                )
                path.moveTo(size.width / 2 - tailPx, tailPx)
                path.lineTo(size.width / 2, 0f)
                path.lineTo(size.width / 2 + tailPx, tailPx)
            }

            TailDirection.Left -> {
                path.addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        tailPx,
                        0f,
                        size.width,
                        size.height,
                        CornerRadius(radiusPx),
                    ),
                )
                path.moveTo(tailPx, size.height / 2 - tailPx)
                path.lineTo(0f, size.height / 2)
                path.lineTo(tailPx, size.height / 2 + tailPx)
            }

            TailDirection.Right -> {
                val rectWidth = size.width - tailPx
                path.addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        0f,
                        0f,
                        rectWidth,
                        size.height,
                        CornerRadius(radiusPx),
                    ),
                )
                path.moveTo(rectWidth, size.height / 2 - tailPx)
                path.lineTo(size.width, size.height / 2)
                path.lineTo(rectWidth, size.height / 2 + tailPx)
            }
        }
        path.close()
        return Outline.Generic(path)
    }
}
