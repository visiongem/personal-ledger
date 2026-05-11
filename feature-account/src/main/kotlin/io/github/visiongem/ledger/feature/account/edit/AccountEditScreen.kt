package io.github.visiongem.ledger.feature.account.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.visiongem.ledger.core.ui.component.ViaFillButton
import io.github.visiongem.ledger.core.ui.component.ViaTopBar
import io.github.visiongem.ledger.feature.account.R

@Composable
fun AccountEditScreen(
    accountId: Long?,
    onDone: () -> Unit,
    viewModel: AccountEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(accountId) {
        viewModel.loadIfNeeded(accountId)
    }
    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            ViaTopBar(
                title = stringResource(
                    if (accountId == null) R.string.account_edit_title_add
                    else R.string.account_edit_title_edit
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
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.account_edit_name_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.currencyCode,
                onValueChange = viewModel::onCurrencyChange,
                label = { Text(stringResource(R.string.account_edit_currency_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.openingBalance,
                onValueChange = viewModel::onOpeningBalanceChange,
                label = { Text(stringResource(R.string.account_edit_balance_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            state.errorMessage?.let { message ->
                Text(text = message, color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.fillMaxWidth())
            ViaFillButton(
                text = stringResource(R.string.account_edit_save),
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
                loading = state.saving,
            )
        }
    }
}

