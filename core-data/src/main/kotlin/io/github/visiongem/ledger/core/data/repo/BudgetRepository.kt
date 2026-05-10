package io.github.visiongem.ledger.core.data.repo

import io.github.visiongem.ledger.core.data.domain.Budget
import io.github.visiongem.ledger.core.data.domain.BudgetWithUsage
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun observeByMonth(month: YearMonth): Flow<List<Budget>>
    fun observeByCategoryAndMonth(categoryId: Long, month: YearMonth): Flow<Budget?>
    fun observeBudgetsWithUsage(month: YearMonth): Flow<List<BudgetWithUsage>>
    suspend fun upsert(budget: Budget): Long
    suspend fun deleteById(id: Long)
}
