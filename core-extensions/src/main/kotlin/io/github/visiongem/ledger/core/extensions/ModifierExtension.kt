package io.github.visiongem.ledger.core.extensions

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.unreadDot(
    visible: Boolean,
    color: Color = Color.Red,
    size: Dp = 6.dp,
): Modifier = if (!visible) this else this.drawWithContent {
    drawContent()
    val radius = size.toPx() / 2
    val cx = this.size.width - radius
    val cy = radius
    drawCircle(color = color, radius = radius, center = Offset(cx, cy))
}

fun Modifier.dashedBorder(
    strokeWidth: Dp = 1.dp,
    dashLength: Dp = 4.dp,
    gapLength: Dp = 2.dp,
    color: Color = Color.Black,
): Modifier = drawWithContent {
    drawContent()
    drawRect(
        color = color,
        style = Stroke(
            width = strokeWidth.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(dashLength.toPx(), gapLength.toPx())
            ),
        ),
    )
}

fun Modifier.shadowed(
    elevation: Dp,
    shape: Shape = RectangleShape,
    shadowColor: Color = Color.Black,
): Modifier = this.shadow(
    elevation = elevation,
    shape = shape,
    ambientColor = shadowColor,
    spotColor = shadowColor,
)
