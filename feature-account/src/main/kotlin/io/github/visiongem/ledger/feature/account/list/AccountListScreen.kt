package io.github.visiongem.ledger.feature.account.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.visiongem.ledger.core.ui.component.ViaEmptyPage
import io.github.visiongem.ledger.core.ui.component.ViaLoadingPage
import io.github.visiongem.ledger.core.ui.component.ViaTopBar
import io.github.visiongem.ledger.core.utils.CurrencyFormatter
import io.github.visiongem.ledger.feature.account.R
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountListScreen(
    onAccountClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    viewModel: AccountListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val archivedMessage = stringResource(R.string.account_archive_undo_message)
    val undoLabel = stringResource(R.string.account_archive_undo_action)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.archiveEvents.collectLatest { archived ->
            val result = snackbarHostState.showSnackbar(
                message = archivedMessage,
                actionLabel = undoLabel,
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoArchive(archived)
            }
        }
    }

    Scaffold(
        topBar = { ViaTopBar(title = stringResource(R.string.account_list_title)) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.account_list_add_cd))
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
                    message = stringResource(R.string.account_list_empty),
                )
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.accounts, key = { it.id }) { account ->
                        ListItem(
                            headlineContent = { Text(account.name) },
                            supportingContent = {
                                Text(CurrencyFormatter.format(account.openingBalance, account.currencyCode))
                            },
                            modifier = Modifier.combinedClickable(
                                onClick = { onAccountClick(account.id) },
                                onLongClick = { viewModel.onLongPress(account) },
                            ),
                        )
                    }
                }
            }
        }
    }
}
