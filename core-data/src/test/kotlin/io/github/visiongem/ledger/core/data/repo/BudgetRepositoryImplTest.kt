package io.github.visiongem.ledger.core.data.repo

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.visiongem.ledger.core.data.domain.Budget
import io.github.visiongem.ledger.core.data.domain.RecordType
import io.github.visiongem.ledger.core.data.local.dao.BudgetDao
import io.github.visiongem.ledger.core.data.local.entity.BudgetEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import java.time.YearMonth
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class BudgetRepositoryImplTest {

    private val budgetDao = mockk<BudgetDao>()
    private val recordRepo = mockk<RecordRepository>()
    private val repo: BudgetRepository = BudgetRepositoryImpl(budgetDao, recordRepo)

    private val month = YearMonth.of(2026, 5)

    private val foodBudget = BudgetEntity(
        id = 1L,
        categoryId = 10L,
        month = month,
        limit = BigDecimal("500.00"),
        currencyCode = "USD",
    )

    private val transportBudget = BudgetEntity(
        id = 2L,
        categoryId = 20L,
        month = month,
        limit = BigDecimal("300.00"),
        currencyCode = "USD",
    )

    @Test
    fun observeByMonthMapsToDomain() = runTest {
        every { budgetDao.observeByMonth(month) } returns flowOf(listOf(foodBudget))
        repo.observeByMonth(month).test {
            val list = awaitItem()
            assertThat(list).hasSize(1)
            assertThat(list[0].limit).isEqualTo(BigDecimal("500.00"))
            awaitComplete()
        }
    }

    @Test
    fun observeBudgetsWithUsageCombinesBudgetAndCategoryTotals() = runTest {
        every { budgetDao.observeByMonth(month) } returns
            flowOf(listOf(foodBudget, transportBudget))
        every {
            recordRepo.observeCategoryTotalsInRange(
                RecordType.EXPENSE,
                month.atDay(1),
                month.atEndOfMonth(),
            )
        } returns flowOf(mapOf(10L to BigDecimal("123.45")))

        repo.observeBudgetsWithUsage(month).test {
            val list = awaitItem()
            assertThat(list).hasSize(2)
            val foodSlot = list.first { it.budget.categoryId == 10L }
            assertThat(foodSlot.usage).isEqualTo(BigDecimal("123.45"))
            val transportSlot = list.first { it.budget.categoryId == 20L }
            assertThat(transportSlot.usage).isEqualTo(BigDecimal.ZERO)
            awaitComplete()
        }
    }

    @Test
    fun upsertConvertsDomainToEntity() = runTest {
        coEvery { budgetDao.upsert(any()) } returns 5L
        val domain = Budget(
            id = 0L,
            categoryId = 10L,
            month = month,
            limit = BigDecimal("500.00"),
            currencyCode = "USD",
        )
        assertThat(repo.upsert(domain)).isEqualTo(5L)
        coVerify { budgetDao.upsert(match { it.categoryId == 10L && it.currencyCode == "USD" }) }
    }

    @Test
    fun deleteByIdDelegates() = runTest {
        coEvery { budgetDao.deleteById(any()) } returns Unit
        repo.deleteById(7L)
        coVerify { budgetDao.deleteById(7L) }
    }
}
