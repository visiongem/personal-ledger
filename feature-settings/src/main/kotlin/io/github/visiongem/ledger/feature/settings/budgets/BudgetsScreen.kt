package io.github.visiongem.ledger.feature.settings.budgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.visiongem.ledger.core.data.domain.Category
import io.github.visiongem.ledger.core.ui.component.ViaTopBar
import io.github.visiongem.ledger.core.utils.CurrencyFormatter
import io.github.visiongem.ledger.feature.settings.R
import java.math.BigDecimal

@Composable
fun BudgetsScreen(
    onBack: () -> Unit,
    viewModel: BudgetsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<BudgetRow?>(null) }

    Scaffold(
        topBar = {
            ViaTopBar(title = stringResource(R.string.budgets_title), onBack = onBack)
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.rows, key = { it.category.id }) { row ->
                    BudgetRowItem(
                        row = row,
                        currency = state.defaultCurrency,
                        onEdit = { editing = row },
                    )
                }
            }
        }
    }

    editing?.let { row ->
        BudgetEditDialog(
            category = row.category,
            currency = state.defaultCurrency,
            initialAmount = row.budgetWithUsage?.budget?.limit,
            onDismiss = { editing = null },
            onSave = { amount ->
                viewModel.setBudget(row.category.id, amount)
                editing = null
            },
            onClear = {
                viewModel.clearBudget(row.category.id)
                editing = null
            },
        )
    }
}

@Composable
private fun BudgetRowItem(
    row: BudgetRow,
    currency: String,
    onEdit: () -> Unit,
) {
    val budget = row.budgetWithUsage?.budget
    val usage = row.budgetWithUsage?.usage ?: BigDecimal.ZERO
    val limit = budget?.limit

    ListItem(
        headlineContent = { Text(row.category.name) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (limit != null) {
                    val used = CurrencyFormatter.format(usage, currency)
                    val ceiling = CurrencyFormatter.format(limit, currency)
                    Text(
                        text = stringResource(R.string.budgets_used_fmt, used, ceiling),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    val progress = if (limit.signum() == 0) 0f
                    else (usage.toDouble() / limit.toDouble()).coerceIn(0.0, 1.0).toFloat()
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Text(
                        text = stringResource(R.string.budgets_no_limit),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        trailingContent = {
            TextButton(onClick = onEdit) {
                Text(stringResource(if (limit != null) R.string.budgets_edit else R.string.budgets_set))
            }
        },
    )
}

@Composable
private fun BudgetEditDialog(
    category: Category,
    currency: String,
    initialAmount: BigDecimal?,
    onDismiss: () -> Unit,
    onSave: (BigDecimal) -> Unit,
    onClear: () -> Unit,
) {
    var input by remember { mutableStateOf(initialAmount?.toPlainString().orEmpty()) }
    val parsed = runCatching { BigDecimal(input) }.getOrNull()
    val canSave = parsed != null && parsed.signum() > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(category.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.budgets_limit_currency_fmt, currency),
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = { parsed?.let(onSave) },
            ) { Text(stringResource(R.string.budgets_save)) }
        },
        dismissButton = {
            if (initialAmount != null) {
                TextButton(onClick = onClear) {
                    Text(stringResource(R.string.budgets_clear))
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        },
    )
}
