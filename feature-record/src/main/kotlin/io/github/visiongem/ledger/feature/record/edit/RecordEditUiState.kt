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
    val targetAccountId: Long? = null,
    val transferAmount: String = "",
    val amount: String = "",
    val dateInput: String = LocalDate.now().toString(),
    val note: String = "",
    val accountOptions: List<Account> = emptyList(),
    val categoryOptions: List<Category> = emptyList(),
    val saving: Boolean = false,
    val saved: Boolean = false,
    val errorMessage: String? = null,
) {
    // True when source/target accounts use different currencies — caller must supply transferAmount.
    val crossCurrencyTransfer: Boolean
        get() {
            if (type != RecordType.TRANSFER) return false
            val source = accountOptions.firstOrNull { it.id == accountId } ?: return false
            val target = accountOptions.firstOrNull { it.id == targetAccountId } ?: return false
            return source.currencyCode != target.currencyCode
        }
}
