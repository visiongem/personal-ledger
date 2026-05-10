package io.github.visiongem.ledger.feature.account.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.visiongem.ledger.core.ui.component.ViaEmptyPage
import io.github.visiongem.ledger.core.ui.component.ViaLoadingPage
import io.github.visiongem.ledger.core.ui.component.ViaTopBar

@Composable
fun AccountListScreen(
    onAccountClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    viewModel: AccountListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { ViaTopBar(title = "Accounts") },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Add account")
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                state.loading -> ViaLoadingPage()
                state.accounts.isEmpty() -> ViaEmptyPage(
                    message = "No accounts yet. Tap + to add your first account.",
                )
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.accounts, key = { it.id }) { account ->
                        ListItem(
                            headlineContent = { Text(account.name) },
                            supportingContent = {
                                Text("${account.currencyCode} · ${account.openingBalance.toPlainString()}")
                            },
                            modifier = Modifier.clickable { onAccountClick(account.id) },
                        )
                    }
                }
            }
        }
    }
}
