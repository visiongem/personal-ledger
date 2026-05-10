package io.github.visiongem.ledger.feature.record.list

import io.github.visiongem.ledger.core.data.domain.Record

data class RecordRow(
    val record: Record,
    val accountName: String,
    val categoryName: String?,
    val displayCurrency: String,
)

data class RecordListUiState(
    val rows: List<RecordRow> = emptyList(),
    val loading: Boolean = true,
)
