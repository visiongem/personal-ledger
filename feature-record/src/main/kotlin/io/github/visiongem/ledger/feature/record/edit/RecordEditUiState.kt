package io.github.visiongem.ledger.feature.record.edit

import io.github.visiongem.ledger.core.data.domain.Account
import io.github.visiongem.ledger.core.data.domain.Category
import io.github.visiongem.ledger.core.data.domain.RecordType
import java.time.LocalDate

data class RecordEditUiState(
    val id: Long? = null,
    val type: RecordType = RecordType.EXPENSE,
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val amount: String = "",
    val dateInput: String = LocalDate.now().toString(),
    val note: String = "",
    val accountOptions: List<Account> = emptyList(),
    val categoryOptions: List<Category> = emptyList(),
    val saving: Boolean = false,
    val saved: Boolean = false,
    val errorMessage: String? = null,
)
