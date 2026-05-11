package io.github.visiongem.ledger.feature.record.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import io.github.visiongem.ledger.core.data.domain.Account
import io.github.visiongem.ledger.core.data.domain.Category
import io.github.visiongem.ledger.core.data.domain.RecordType
import io.github.visiongem.ledger.core.ui.component.ViaBottomSelector
import io.github.visiongem.ledger.core.ui.component.ViaFillButton
import io.github.visiongem.ledger.core.ui.component.ViaSelectorField
import io.github.visiongem.ledger.core.ui.component.ViaTopBar
import io.github.visiongem.ledger.feature.record.R

@Composable
fun RecordEditScreen(
    recordId: Long?,
    onDone: () -> Unit,
    viewModel: RecordEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    LaunchedEffect(recordId) { viewModel.loadIfNeeded(recordId) }
    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    Scaffold(
        topBar = {
            ViaTopBar(
                title = stringResource(
                    if (recordId == null) R.string.record_edit_title_add
                    else R.string.record_edit_title_edit
                ),
                onBack = onDone,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TypeSegmentedButtons(state.type, viewModel::onTypeChange)
            AccountPicker(
                label = stringResource(
                    if (state.type == RecordType.TRANSFER) R.string.record_from_account
                    else R.string.record_account_label
                ),
                accounts = state.accountOptions,
                selectedId = state.accountId,
                onSelect = viewModel::onAccountChange,
            )
            if (state.type == RecordType.TRANSFER) {
                AccountPicker(
                    label = stringResource(R.string.record_to_account),
                    accounts = state.accountOptions.filter { it.id != state.accountId },
                    selectedId = state.targetAccountId,
                    onSelect = viewModel::onTargetAccountChange,
                )
            } else {
                CategoryPicker(
                    categories = state.categoryOptions,
                    selectedId = state.categoryId,
                    onSelect = viewModel::onCategoryChange,
                )
            }
            OutlinedTextField(
                value = state.amount,
                onValueChange = viewModel::onAmountChange,
                label = {
                    Text(
                        stringResource(
                            if (state.type == RecordType.TRANSFER) R.string.record_amount_source
                            else R.string.record_amount
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            if (state.crossCurrencyTransfer) {
                val targetCurrency = state.accountOptions
                    .firstOrNull { it.id == state.targetAccountId }
                    ?.currencyCode
                    .orEmpty()
                OutlinedTextField(
                    value = state.transferAmount,
                    onValueChange = viewModel::onTransferAmountChange,
                    label = {
                        Text(stringResource(R.string.record_amount_destination_fmt, targetCurrency))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
            OutlinedTextField(
                value = state.dateInput,
                onValueChange = viewModel::onDateChange,
                label = { Text(stringResource(R.string.record_date_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::onNoteChange,
                label = { Text(stringResource(R.string.record_note_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
            )
            state.errorMessage?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error)
            }
            ViaFillButton(
                text = stringResource(R.string.record_save),
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
                loading = state.saving,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypeSegmentedButtons(
    selected: RecordType,
    onChange: (RecordType) -> Unit,
) {
    val visibleTypes = listOf(RecordType.EXPENSE, RecordType.INCOME, RecordType.TRANSFER)
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        visibleTypes.forEachIndexed { index, type ->
            SegmentedButton(
                selected = type == selected,
                onClick = { onChange(type) },
                shape = SegmentedButtonDefaults.itemShape(index, visibleTypes.size),
            ) {
                Text(
                    stringResource(
                        when (type) {
                            RecordType.EXPENSE -> R.string.record_type_expense
                            RecordType.INCOME -> R.string.record_type_income
                            RecordType.TRANSFER -> R.string.record_type_transfer
                        }
                    )
                )
            }
        }
    }
}

@Composable
private fun AccountPicker(
    label: String,
    accounts: List<Account>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
) {
    var sheetOpen by remember { mutableStateOf(false) }
    val selected = accounts.firstOrNull { it.id == selectedId }
    ViaSelectorField(
        label = label,
        valueLabel = selected?.let { "${it.name} (${it.currencyCode})" },
        onClick = { sheetOpen = true },
    )
    if (sheetOpen) {
        ViaBottomSelector(
            title = label,
            items = accounts,
            itemLabel = { "${it.name} (${it.currencyCode})" },
            onSelect = { onSelect(it.id) },
            onDismiss = { sheetOpen = false },
        )
    }
}

@Composable
private fun CategoryPicker(
    categories: List<Category>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
) {
    val label = stringResource(R.string.record_category)
    var sheetOpen by remember { mutableStateOf(false) }
    val selected = categories.firstOrNull { it.id == selectedId }
    ViaSelectorField(
        label = label,
        valueLabel = selected?.name,
        onClick = { sheetOpen = true },
    )
    if (sheetOpen) {
        ViaBottomSelector(
            title = label,
            items = categories,
            itemLabel = { it.name },
            onSelect = { onSelect(it.id) },
            onDismiss = { sheetOpen = false },
        )
    }
}
