package io.github.visiongem.ledger.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.math.BigDecimal

data class BarEntry(
    val label: String,
    val value: BigDecimal,
    val valueText: String = value.toPlainString(),
)

@Composable
fun ViaHorizontalBarChart(
    entries: List<BarEntry>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primaryContainer,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    if (entries.isEmpty()) return
    val maxValue = entries.maxOf { it.value }
    if (maxValue.signum() == 0) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        entries.forEach { entry ->
            val fraction = entry.value.toDouble()
                .div(maxValue.toDouble())
                .toFloat()
                .coerceIn(0f, 1f)
            BarRow(entry.label, entry.valueText, fraction, barColor, trackColor)
        }
    }
}

@Composable
private fun BarRow(
    label: String,
    valueText: String,
    fraction: Float,
    barColor: Color,
    trackColor: Color,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(valueText, style = MaterialTheme.typography.labelMedium)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BarHeight)
                .background(trackColor, BarShape),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(barColor, BarShape),
            )
        }
    }
}

private val BarHeight = 8.dp
private val BarShape = RoundedCornerShape(4.dp)
