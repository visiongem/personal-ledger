package io.github.visiongem.ledger.feature.settings.category

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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.visiongem.ledger.core.ui.component.ViaTopBar
import io.github.visiongem.ledger.feature.settings.R
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryListScreen(
    onCategoryClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    onBack: () -> Unit,
    viewModel: CategoryListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val deletedMessage = stringResource(R.string.category_delete_undo_message)
    val undoLabel = stringResource(R.string.category_delete_undo_action)
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
        topBar = {
            ViaTopBar(
                title = stringResource(R.string.category_list_title),
                onBack = onBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.category_list_add_cd))
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (state.expense.isNotEmpty()) {
                    item("expense_header") {
                        SectionHeader(stringResource(R.string.category_section_expense))
                    }
                    items(state.expense, key = { "exp_${it.id}" }) { category ->
                        ListItem(
                            headlineContent = { Text(category.name) },
                            modifier = Modifier.combinedClickable(
                                onClick = { onCategoryClick(category.id) },
                                onLongClick = { viewModel.onLongPress(category) },
                            ),
                        )
                    }
                }
                if (state.income.isNotEmpty()) {
                    item("income_header") {
                        SectionHeader(stringResource(R.string.category_section_income))
                    }
                    items(state.income, key = { "inc_${it.id}" }) { category ->
                        ListItem(
                            headlineContent = { Text(category.name) },
                            modifier = Modifier.combinedClickable(
                                onClick = { onCategoryClick(category.id) },
                                onLongClick = { viewModel.onLongPress(category) },
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
