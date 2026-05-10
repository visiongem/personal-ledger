package io.github.visiongem.ledger.feature.record.list

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
import io.github.visiongem.ledger.core.data.domain.RecordType
import io.github.visiongem.ledger.core.ui.component.ViaEmptyPage
import io.github.visiongem.ledger.core.ui.component.ViaLoadingPage
import io.github.visiongem.ledger.core.ui.component.ViaTopBar

@Composable
fun RecordListScreen(
    onRecordClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    viewModel: RecordListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { ViaTopBar(title = "Records") },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Add record")
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
                state.rows.isEmpty() -> ViaEmptyPage(
                    message = "No records yet. Tap + to add your first one.",
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
                                Text("$sign ${row.displayCurrency} ${row.record.amount.toPlainString()}")
                            },
                            supportingContent = {
                                val cat = row.categoryName ?: "(no category)"
                                Text("$cat · ${row.accountName} · ${row.record.occurredOn}")
                            },
                            modifier = Modifier.clickable { onRecordClick(row.record.id) },
                        )
                    }
                }
            }
        }
    }
}
