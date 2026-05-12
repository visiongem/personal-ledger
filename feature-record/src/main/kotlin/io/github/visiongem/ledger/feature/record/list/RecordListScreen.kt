package io.github.visiongem.ledger.feature.record.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.visiongem.ledger.core.data.domain.Account
import io.github.visiongem.ledger.core.data.domain.RecordType
import io.github.visiongem.ledger.core.ui.component.ViaBottomSelector
import io.github.visiongem.ledger.core.ui.component.ViaEmptyPage
import io.github.visiongem.ledger.core.ui.component.ViaLoadingPage
import io.github.visiongem.ledger.core.ui.component.ViaTopBar
import io.github.visiongem.ledger.core.utils.CurrencyFormatter
import io.github.visiongem.ledger.feature.record.R
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RecordListScreen(
    onRecordClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    viewModel: RecordListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val noCategoryLabel = stringResource(R.string.record_list_no_category)
    val unknownAccountLabel = stringResource(R.string.record_list_unknown_account)
    val deletedMessage = stringResource(R.string.record_delete_undo_message)
    val undoLabel = stringResource(R.string.record_delete_undo_action)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.deletionEvents.collectLatest { deleted ->
            val result = snackbarHostState.showSnackbar(
                message = deletedMessage,
                actionLabel = undoLabel,
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDelete(deleted)
            }
        }
    }

    Scaffold(
        topBar = { ViaTopBar(title = stringResource(R.string.record_list_title)) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.record_list_add_cd))
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            FilterRow(
                selectedType = state.selectedType,
                accounts = state.accounts,
                selectedAccountId = state.selectedAccountId,
                onTypeChange = viewModel::onTypeFilterChange,
                onAccountChange = viewModel::onAccountFilterChange,
            )
            when {
                state.loading -> ViaLoadingPage()
                state.rows.isEmpty() -> ViaEmptyPage(
                    message = stringResource(R.string.record_list_empty),
                )
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.rows, key = { it.record.id }) { row ->
                        ListItem(
                            headlineContent = {
                                val sign = when (row.record.type) {
                                    RecordType.INCOME -> "+"
                                    RecordType.EXPENSE -> "-"
                                    RecordType.TRANSFER -> "→"
                                }
                                Text("$sign ${CurrencyFormatter.format(row.record.amount, row.displayCurrency)}")
                            },
                            supportingContent = {
                                val text = if (row.record.type == RecordType.TRANSFER) {
                                    val target = row.targetAccountName ?: unknownAccountLabel
                                    "${row.accountName} → $target · ${row.record.occurredOn}"
                                } else {
                                    val cat = row.categoryName ?: noCategoryLabel
                                    "$cat · ${row.accountName} · ${row.record.occurredOn}"
                                }
                                Text(text)
                            },
                            modifier = Modifier.combinedClickable(
                                onClick = { onRecordClick(row.record.id) },
                                onLongClick = { viewModel.onLongPress(row.record) },
                            ),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterRow(
    selectedType: RecordType?,
    accounts: List<Account>,
    selectedAccountId: Long?,
    onTypeChange: (RecordType?) -> Unit,
    onAccountChange: (Long?) -> Unit,
) {
    val allLabel = stringResource(R.string.record_filter_all)
    val accountChipLabel = accounts.firstOrNull { it.id == selectedAccountId }?.name
        ?: stringResource(R.string.record_filter_account)
    var accountSheetOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedType == null,
            onClick = { onTypeChange(null) },
            label = { Text(allLabel) },
        )
        FilterChip(
            selected = selectedType == RecordType.EXPENSE,
            onClick = { onTypeChange(RecordType.EXPENSE) },
            label = { Text(stringResource(R.string.record_type_expense)) },
        )
        FilterChip(
            selected = selectedType == RecordType.INCOME,
            onClick = { onTypeChange(RecordType.INCOME) },
            label = { Text(stringResource(R.string.record_type_income)) },
        )
        FilterChip(
            selected = selectedType == RecordType.TRANSFER,
            onClick = { onTypeChange(RecordType.TRANSFER) },
            label = { Text(stringResource(R.string.record_type_transfer)) },
        )
        FilterChip(
            selected = selectedAccountId != null,
            onClick = { accountSheetOpen = true },
            label = { Text(accountChipLabel) },
        )
    }

    if (accountSheetOpen) {
        // Synthetic "All accounts" item with id = 0 so the picker can express
        // clearing the filter alongside the real account choices.
        val all = Account(
            id = 0L,
            name = allLabel,
            currencyCode = "",
            createdAt = java.time.Instant.EPOCH,
        )
        ViaBottomSelector(
            title = stringResource(R.string.record_filter_account),
            items = listOf(all) + accounts,
            itemLabel = { a -> if (a.id == 0L) a.name else "${a.name} (${a.currencyCode})" },
            onSelect = { picked -> onAccountChange(if (picked.id == 0L) null else picked.id) },
            onDismiss = { accountSheetOpen = false },
        )
    }
}
