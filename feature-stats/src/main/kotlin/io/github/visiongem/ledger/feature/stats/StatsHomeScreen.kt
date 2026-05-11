package io.github.visiongem.ledger.feature.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.visiongem.ledger.core.ui.component.BarEntry
import io.github.visiongem.ledger.core.ui.component.ViaHorizontalBarChart
import io.github.visiongem.ledger.core.ui.component.ViaLoadingPage
import io.github.visiongem.ledger.core.ui.component.ViaTopBar
import java.math.BigDecimal

@Composable
fun StatsHomeScreen(
    viewModel: StatsHomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { ViaTopBar(title = "Statistics · ${state.month}") },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            if (state.loading) {
                ViaLoadingPage()
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (state.unconvertedCount > 0) {
                        Text(
                            text = "${state.unconvertedCount} record(s) skipped — exchange rate missing. Refresh from Settings.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    SummaryCards(state)
                    HorizontalDivider()
                    Text(
                        text = "Expense breakdown",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (state.expenseByCategory.isEmpty()) {
                        Text(
                            text = "No expenses this month yet.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        ViaHorizontalBarChart(
                            entries = state.expenseByCategory.map { entry ->
                                BarEntry(
                                    label = entry.categoryName,
                                    value = entry.total,
                                    valueText = "${state.displayCurrency} ${entry.total.toPlainString()}",
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCards(state: StatsHomeUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SummaryCard("Income", state.incomeTotal, state.displayCurrency, Modifier.weight(1f))
        SummaryCard("Expense", state.expenseTotal, state.displayCurrency, Modifier.weight(1f))
        SummaryCard("Net", state.net, state.displayCurrency, Modifier.weight(1f))
    }
}

@Composable
private fun SummaryCard(
    label: String,
    amount: BigDecimal,
    currency: String,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(
                text = "$currency ${amount.toPlainString()}",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
