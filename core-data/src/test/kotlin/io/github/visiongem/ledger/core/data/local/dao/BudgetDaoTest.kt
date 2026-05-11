package io.github.visiongem.ledger.core.data.local.dao

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.visiongem.ledger.core.data.domain.CategoryType
import io.github.visiongem.ledger.core.data.local.AppDatabase
import io.github.visiongem.ledger.core.data.local.InMemoryDatabaseFactory
import io.github.visiongem.ledger.core.data.local.entity.BudgetEntity
import io.github.visiongem.ledger.core.data.local.entity.CategoryEntity
import java.math.BigDecimal
import java.time.YearMonth
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BudgetDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var budgetDao: BudgetDao
    private lateinit var categoryDao: CategoryDao
    private var foodCategoryId = 0L
    private var transportCategoryId = 0L

    @Before
    fun setUp() = runTest {
        db = InMemoryDatabaseFactory.create()
        budgetDao = db.budgetDao()
        categoryDao = db.categoryDao()
        foodCategoryId = categoryDao.upsert(category("Food"))
        transportCategoryId = categoryDao.upsert(category("Transport"))
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun observeByMonthReturnsBudgetsForThatMonth() = runTest {
        val may = YearMonth.of(2026, 5)
        val june = YearMonth.of(2026, 6)
        budgetDao.upsert(budget(foodCategoryId, may, "500"))
        budgetDao.upsert(budget(transportCategoryId, may, "300"))
        budgetDao.upsert(budget(foodCategoryId, june, "600"))

        budgetDao.observeByMonth(may).test {
            val list = awaitItem()
            assertThat(list).hasSize(2)
            assertThat(list.map { it.limit.toPlainString() }).containsExactly("500", "300")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun upsertSameCategoryAndMonthReplaces() = runTest {
        val may = YearMonth.of(2026, 5)
        val firstId = budgetDao.upsert(budget(foodCategoryId, may, "500"))
        // Unique index (categoryId, month) means @Upsert updates the existing row.
        budgetDao.upsert(budget(foodCategoryId, may, "800").copy(id = firstId))

        budgetDao.observeByMonth(may).test {
            val list = awaitItem()
            assertThat(list).hasSize(1)
            assertThat(list[0].limit).isEqualTo(BigDecimal("800"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deletingCategoryCascadesAndRemovesBudget() = runTest {
        val may = YearMonth.of(2026, 5)
        val id = budgetDao.upsert(budget(foodCategoryId, may, "500"))

        categoryDao.deleteById(foodCategoryId)

        budgetDao.observeById(id).test {
            assertThat(awaitItem()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun category(name: String) =
        CategoryEntity(0L, name, CategoryType.EXPENSE, null, 0)

    private fun budget(categoryId: Long, month: YearMonth, limit: String) = BudgetEntity(
        id = 0L,
        categoryId = categoryId,
        month = month,
        limit = BigDecimal(limit),
        currencyCode = "USD",
    )
}
