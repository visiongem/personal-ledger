package io.github.visiongem.ledger.core.data.local.dao

import android.database.sqlite.SQLiteConstraintException
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.visiongem.ledger.core.data.domain.CategoryType
import io.github.visiongem.ledger.core.data.domain.RecordType
import io.github.visiongem.ledger.core.data.local.AppDatabase
import io.github.visiongem.ledger.core.data.local.InMemoryDatabaseFactory
import io.github.visiongem.ledger.core.data.local.entity.AccountEntity
import io.github.visiongem.ledger.core.data.local.entity.CategoryEntity
import io.github.visiongem.ledger.core.data.local.entity.RecordEntity
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RecordDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var recordDao: RecordDao
    private lateinit var accountDao: AccountDao
    private lateinit var categoryDao: CategoryDao
    private var sourceAccountId = 0L
    private var targetAccountId = 0L
    private var foodCategoryId = 0L

    @Before
    fun setUp() = runTest {
        db = InMemoryDatabaseFactory.create()
        recordDao = db.recordDao()
        accountDao = db.accountDao()
        categoryDao = db.categoryDao()

        sourceAccountId = accountDao.upsert(account("Cash"))
        targetAccountId = accountDao.upsert(account("Savings"))
        foodCategoryId = categoryDao.upsert(category("Food", CategoryType.EXPENSE))
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun observeInRangeReturnsRecordsWithinBounds() = runTest {
        recordDao.upsert(expense(LocalDate.of(2026, 5, 10), foodCategoryId, "10"))
        recordDao.upsert(expense(LocalDate.of(2026, 6, 15), foodCategoryId, "20"))
        recordDao.upsert(expense(LocalDate.of(2026, 4, 30), foodCategoryId, "5"))

        recordDao.observeInRange(
            LocalDate.of(2026, 5, 1),
            LocalDate.of(2026, 5, 31),
        ).test {
            val list = awaitItem()
            assertThat(list.map { it.amount.toPlainString() }).containsExactly("10")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeByAccountFiltersToOneAccount() = runTest {
        recordDao.upsert(expense(LocalDate.of(2026, 5, 10), foodCategoryId, "10"))
        recordDao.upsert(
            expense(LocalDate.of(2026, 5, 10), foodCategoryId, "20")
                .copy(accountId = targetAccountId)
        )

        recordDao.observeByAccount(sourceAccountId).test {
            val list = awaitItem()
            assertThat(list).hasSize(1)
            assertThat(list[0].amount).isEqualTo(BigDecimal("10"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeByCategoryInRangeFiltersBoth() = runTest {
        val transportId = categoryDao.upsert(category("Transport", CategoryType.EXPENSE))
        recordDao.upsert(expense(LocalDate.of(2026, 5, 10), foodCategoryId, "10"))
        recordDao.upsert(expense(LocalDate.of(2026, 5, 12), transportId, "30"))
        recordDao.upsert(expense(LocalDate.of(2026, 6, 1), foodCategoryId, "100"))

        recordDao.observeByCategoryInRange(
            categoryId = foodCategoryId,
            startInclusive = LocalDate.of(2026, 5, 1),
            endInclusive = LocalDate.of(2026, 5, 31),
        ).test {
            val list = awaitItem()
            assertThat(list).hasSize(1)
            assertThat(list[0].amount).isEqualTo(BigDecimal("10"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deletingAccountThatHasRecordsIsBlockedByFkRestrict() = runTest {
        recordDao.upsert(expense(LocalDate.of(2026, 5, 10), foodCategoryId, "10"))
        assertThrows(SQLiteConstraintException::class.java) {
            kotlinx.coroutines.runBlocking { accountDao.deleteById(sourceAccountId) }
        }
    }

    @Test
    fun deletingCategoryNullsRecordCategoryViaSetNull() = runTest {
        val recordId = recordDao.upsert(expense(LocalDate.of(2026, 5, 10), foodCategoryId, "10"))

        categoryDao.deleteById(foodCategoryId)

        recordDao.observeById(recordId).test {
            val record = awaitItem()!!
            assertThat(record.categoryId).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun account(name: String) = AccountEntity(
        id = 0L,
        name = name,
        currencyCode = "USD",
        openingBalance = BigDecimal.ZERO,
        archived = false,
        createdAt = Instant.parse("2026-05-10T00:00:00Z"),
    )

    private fun category(name: String, type: CategoryType) =
        CategoryEntity(0L, name, type, null, 0)

    private fun expense(date: LocalDate, categoryId: Long, amount: String) = RecordEntity(
        id = 0L,
        accountId = sourceAccountId,
        categoryId = categoryId,
        type = RecordType.EXPENSE,
        amount = BigDecimal(amount),
        occurredOn = date,
        note = null,
        transferToAccountId = null,
        transferAmount = null,
    )
}
