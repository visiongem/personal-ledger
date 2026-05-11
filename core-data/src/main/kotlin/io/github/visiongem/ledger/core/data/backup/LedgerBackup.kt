package io.github.visiongem.ledger.core.data.backup

import io.github.visiongem.ledger.core.data.domain.Account
import io.github.visiongem.ledger.core.data.domain.Budget
import io.github.visiongem.ledger.core.data.domain.Category
import io.github.visiongem.ledger.core.data.domain.ExchangeRate
import io.github.visiongem.ledger.core.data.domain.Record

// Full-schema backup snapshot — five lists, one per top-level entity.
data class LedgerBackup(
    val accounts: List<Account>,
    val categories: List<Category>,
    val records: List<Record>,
    val budgets: List<Budget>,
    val rates: List<ExchangeRate>,
) {
    val totalCount: Int get() =
        accounts.size + categories.size + records.size + budgets.size + rates.size
}
