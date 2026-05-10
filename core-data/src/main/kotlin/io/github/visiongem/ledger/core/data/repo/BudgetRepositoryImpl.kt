package io.github.visiongem.ledger.core.data.repo

import io.github.visiongem.ledger.core.data.domain.Budget
import io.github.visiongem.ledger.core.data.domain.BudgetWithUsage
import io.github.visiongem.ledger.core.data.domain.RecordType
import io.github.visiongem.ledger.core.data.local.dao.BudgetDao
import io.github.visiongem.ledger.core.data.local.mapper.toDomain
import io.github.visiongem.ledger.core.data.local.mapper.toEntity
import java.math.BigDecimal
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class BudgetRepositoryImpl @Inject constructor(
    private val dao: BudgetDao,
    private val recordRepository: RecordRepository,
) : BudgetRepository {

    override fun observeByMonth(month: YearMonth): Flow<List<Budget>> =
        dao.observeByMonth(month).map { list -> list.map { it.toDomain() } }

    override fun observeByCategoryAndMonth(categoryId: Long, month: YearMonth): Flow<Budget?> =
        dao.observeByCategoryAndMonth(categoryId, month).map { it?.toDomain() }

    override fun observeBudgetsWithUsage(month: YearMonth): Flow<List<BudgetWithUsage>> {
        val budgetsFlow = observeByMonth(month)
        val totalsFlow = recordRepository.observeCategoryTotalsInRange(
            type = RecordType.EXPENSE,
            start = month.atDay(1),
            end = month.atEndOfMonth(),
        )
        return combine(budgetsFlow, totalsFlow) { budgets, totals ->
            budgets.map { b ->
                BudgetWithUsage(
                    budget = b,
                    usage = totals[b.categoryId] ?: BigDecimal.ZERO,
                )
            }
        }
    }

    override suspend fun upsert(budget: Budget): Long = dao.upsert(budget.toEntity())

    override suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }
}
