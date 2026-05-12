package io.github.visiongem.ledger.feature.settings.budgets

import io.github.visiongem.ledger.core.data.domain.BudgetWithUsage
import io.github.visiongem.ledger.core.data.domain.Category
import java.time.YearMonth

data class BudgetRow(
    val category: Category,
    val budgetWithUsage: BudgetWithUsage?,
)

data class BudgetsUiState(
    val month: YearMonth = YearMonth.now(),
    val defaultCurrency: String = "USD",
    val rows: List<BudgetRow> = emptyList(),
    val loading: Boolean = true,
)
