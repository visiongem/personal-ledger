package io.github.visiongem.ledger.core.data.local.dao

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.visiongem.ledger.core.data.domain.CategoryType
import io.github.visiongem.ledger.core.data.local.AppDatabase
import io.github.visiongem.ledger.core.data.local.InMemoryDatabaseFactory
import io.github.visiongem.ledger.core.data.local.entity.CategoryEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CategoryDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: CategoryDao

    @Before
    fun setUp() {
        db = InMemoryDatabaseFactory.create()
        dao = db.categoryDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun observeByTypeFiltersIncomeFromExpense() = runTest {
        dao.upsert(category("Food", CategoryType.EXPENSE))
        dao.upsert(category("Transport", CategoryType.EXPENSE))
        dao.upsert(category("Salary", CategoryType.INCOME))

        dao.observeByType(CategoryType.EXPENSE).test {
            val expenses = awaitItem()
            assertThat(expenses.map { it.name }).containsExactly("Food", "Transport")
            cancelAndIgnoreRemainingEvents()
        }

        dao.observeByType(CategoryType.INCOME).test {
            val incomes = awaitItem()
            assertThat(incomes.map { it.name }).containsExactly("Salary")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeAllOrderedBySortOrderAscending() = runTest {
        dao.upsert(category("Z", sortOrder = 0))
        dao.upsert(category("A", sortOrder = 5))
        dao.upsert(category("M", sortOrder = 2))

        dao.observeAll().test {
            val list = awaitItem()
            assertThat(list.map { it.name }).containsExactly("Z", "M", "A").inOrder()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteByIdRemovesRow() = runTest {
        val id = dao.upsert(category("Temp"))
        dao.deleteById(id)

        dao.observeById(id).test {
            assertThat(awaitItem()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun category(
        name: String,
        type: CategoryType = CategoryType.EXPENSE,
        iconKey: String? = null,
        sortOrder: Int = 0,
    ) = CategoryEntity(0L, name, type, iconKey, sortOrder)
}
