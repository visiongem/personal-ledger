package io.github.visiongem.ledger.feature.record.list

import io.github.visiongem.ledger.core.data.domain.Account
import io.github.visiongem.ledger.core.data.domain.Record
import io.github.visiongem.ledger.core.data.domain.RecordType

data class RecordRow(
    val record: Record,
    val accountName: String,
    val categoryName: String?,
    val displayCurrency: String,
    val targetAccountName: String? = null,
)

data class RecordListUiState(
    val rows: List<RecordRow> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val selectedType: RecordType? = null,
    val selectedAccountId: Long? = null,
    val loading: Boolean = true,
)
