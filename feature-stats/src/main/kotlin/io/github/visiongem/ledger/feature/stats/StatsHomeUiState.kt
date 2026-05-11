package io.github.visiongem.ledger.feature.stats

import java.math.BigDecimal
import java.time.YearMonth

data class CategoryTotal(
    val categoryName: String,
    val total: BigDecimal,
)

data class StatsHomeUiState(
    val month: YearMonth = YearMonth.now(),
    val incomeTotal: BigDecimal = BigDecimal.ZERO,
    val expenseTotal: BigDecimal = BigDecimal.ZERO,
    val net: BigDecimal = BigDecimal.ZERO,
    val expenseByCategory: List<CategoryTotal> = emptyList(),
    val displayCurrency: String = "USD",
    val unconvertedCount: Int = 0,
    val loading: Boolean = true,
)
