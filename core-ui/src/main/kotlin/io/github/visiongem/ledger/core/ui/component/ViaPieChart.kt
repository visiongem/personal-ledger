package io.github.visiongem.ledger.core.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.math.BigDecimal

data class PieSlice(
    val label: String,
    val value: BigDecimal,
    val color: Color,
)

// Compose Canvas donut chart with a side legend. Empty or all-zero data renders nothing.
@Composable
fun ViaPieChart(
    slices: List<PieSlice>,
    modifier: Modifier = Modifier,
    chartSize: Dp = 160.dp,
    strokeWidth: Dp = 24.dp,
) {
    if (slices.isEmpty()) return
    val total = slices.fold(BigDecimal.ZERO) { acc, s -> acc.add(s.value) }
    if (total.signum() == 0) return

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Donut(
            slices = slices,
            total = total,
            chartSize = chartSize,
            strokeWidth = strokeWidth,
        )
        Legend(slices = slices, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Donut(
    slices: List<PieSlice>,
    total: BigDecimal,
    chartSize: Dp,
    strokeWidth: Dp,
) {
    Canvas(modifier = Modifier.size(chartSize)) {
        val stroke = Stroke(width = strokeWidth.toPx())
        val inset = strokeWidth.toPx() / 2
        val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
        val topLeft = Offset(inset, inset)
        var startAngle = -90f // start at 12 o'clock
        for (slice in slices) {
            val sweep = (slice.value.toDouble() / total.toDouble() * 360.0).toFloat()
            drawArc(
                color = slice.color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
            startAngle += sweep
        }
    }
}

@Composable
private fun Legend(slices: List<PieSlice>, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        slices.forEach { slice ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.wrapContentSize(),
            ) {
                Box(
                    modifier = Modifier
                        .size(LEGEND_DOT)
                        .background(color = slice.color, shape = CircleShape),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(slice.label, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private val LEGEND_DOT = 10.dp

// Default rotating palette pulled from MaterialTheme. Callers can override per slice.
@Composable
fun rememberPiePalette(): List<Color> = listOf(
    MaterialTheme.colorScheme.primary,
    MaterialTheme.colorScheme.secondary,
    MaterialTheme.colorScheme.tertiary,
    MaterialTheme.colorScheme.primaryContainer,
    MaterialTheme.colorScheme.secondaryContainer,
    MaterialTheme.colorScheme.tertiaryContainer,
)
