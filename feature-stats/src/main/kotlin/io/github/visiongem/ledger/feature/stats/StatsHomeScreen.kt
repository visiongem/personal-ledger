package io.github.visiongem.ledger.feature.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.visiongem.ledger.core.ui.component.BarEntry
import io.github.visiongem.ledger.core.ui.component.PieSlice
import io.github.visiongem.ledger.core.ui.component.ViaHorizontalBarChart
import io.github.visiongem.ledger.core.ui.component.ViaLoadingPage
import io.github.visiongem.ledger.core.ui.component.ViaPieChart
import io.github.visiongem.ledger.core.ui.component.ViaTopBar
import io.github.visiongem.ledger.core.ui.component.rememberPiePalette
import io.github.visiongem.ledger.core.utils.CurrencyFormatter
import io.github.visiongem.ledger.feature.stats.R
import java.math.BigDecimal

@Composable
fun StatsHomeScreen(
    viewModel: StatsHomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { ViaTopBar(title = stringResource(R.string.stats_title_fmt, state.month.toString())) },
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
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (state.unconvertedCount > 0) {
                        Text(
                            text = stringResource(R.string.stats_warn_unconverted_fmt, state.unconvertedCount),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    SummaryCards(state)
                    HorizontalDivider()
                    Text(
                        text = stringResource(R.string.stats_breakdown_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (state.expenseByCategory.isEmpty()) {
                        Text(
                            text = stringResource(R.string.stats_breakdown_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        val palette = rememberPiePalette()
                        ViaPieChart(
                            slices = state.expenseByCategory.mapIndexed { index, entry ->
                                PieSlice(
                                    label = entry.categoryName,
                                    value = entry.total,
                                    color = palette[index % palette.size],
                                )
                            },
                        )
                        ViaHorizontalBarChart(
                            entries = state.expenseByCategory.map { entry ->
                                BarEntry(
                                    label = entry.categoryName,
                                    value = entry.total,
                                    valueText = CurrencyFormatter.format(entry.total, state.displayCurrency),
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
        SummaryCard(stringResource(R.string.stats_income), state.incomeTotal, state.displayCurrency, Modifier.weight(1f))
        SummaryCard(stringResource(R.string.stats_expense), state.expenseTotal, state.displayCurrency, Modifier.weight(1f))
        SummaryCard(stringResource(R.string.stats_net), state.net, state.displayCurrency, Modifier.weight(1f))
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
                text = CurrencyFormatter.format(amount, currency),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
