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
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.visiongem.ledger.core.data.domain.RecordType
import io.github.visiongem.ledger.core.ui.component.ViaEmptyPage
import io.github.visiongem.ledger.core.ui.component.ViaLoadingPage
import io.github.visiongem.ledger.core.ui.component.ViaTopBar
import io.github.visiongem.ledger.feature.record.R

@Composable
fun RecordListScreen(
    onRecordClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    viewModel: RecordListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val noCategoryLabel = stringResource(R.string.record_list_no_category)
    val unknownAccountLabel = stringResource(R.string.record_list_unknown_account)

    Scaffold(
        topBar = { ViaTopBar(title = stringResource(R.string.record_list_title)) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.record_list_add_cd))
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
                                Text("$sign ${row.displayCurrency} ${row.record.amount.toPlainString()}")
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
                            modifier = Modifier.clickable { onRecordClick(row.record.id) },
                        )
                    }
                }
            }
        }
    }
}
