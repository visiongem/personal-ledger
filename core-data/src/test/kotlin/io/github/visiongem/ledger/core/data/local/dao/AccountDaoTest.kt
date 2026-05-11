package io.github.visiongem.ledger.core.data.local.dao

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.visiongem.ledger.core.data.local.AppDatabase
import io.github.visiongem.ledger.core.data.local.InMemoryDatabaseFactory
import io.github.visiongem.ledger.core.data.local.entity.AccountEntity
import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AccountDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: AccountDao

    @Before
    fun setUp() {
        db = InMemoryDatabaseFactory.create()
        dao = db.accountDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun upsertInsertsAndAllowsLookupById() = runTest {
        val id = dao.upsert(account(name = "Cash"))
        assertThat(id).isGreaterThan(0L)

        dao.observeById(id).test {
            val retrieved = awaitItem()!!
            assertThat(retrieved.name).isEqualTo("Cash")
            assertThat(retrieved.currencyCode).isEqualTo("USD")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeActiveExcludesArchived() = runTest {
        dao.upsert(account(name = "Active", archived = false))
        dao.upsert(account(name = "Old", archived = true))

        dao.observeActive().test {
            val list = awaitItem()
            assertThat(list.map { it.name }).containsExactly("Active")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteByIdRemovesRow() = runTest {
        val id = dao.upsert(account(name = "Temp"))
        dao.deleteById(id)

        dao.observeById(id).test {
            assertThat(awaitItem()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeAllReturnsRowsOrderedByCreatedAtDescending() = runTest {
        dao.upsert(account(name = "Older", createdAt = Instant.parse("2026-05-10T00:00:00Z")))
        dao.upsert(account(name = "Newer", createdAt = Instant.parse("2026-05-11T00:00:00Z")))

        dao.observeAll().test {
            val list = awaitItem()
            assertThat(list.map { it.name }).containsExactly("Newer", "Older").inOrder()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun upsertWithExistingIdReplaces() = runTest {
        val id = dao.upsert(account(name = "Original"))
        // Upsert returns -1 on UPDATE (no new rowid), so verify by re-read.
        dao.upsert(account(name = "Renamed", openingBalance = BigDecimal("50")).copy(id = id))

        dao.observeById(id).test {
            val retrieved = awaitItem()!!
            assertThat(retrieved.name).isEqualTo("Renamed")
            assertThat(retrieved.openingBalance).isEqualTo(BigDecimal("50"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun account(
        name: String,
        currencyCode: String = "USD",
        openingBalance: BigDecimal = BigDecimal.ZERO,
        archived: Boolean = false,
        createdAt: Instant = Instant.parse("2026-05-10T00:00:00Z"),
    ) = AccountEntity(0L, name, currencyCode, openingBalance, archived, createdAt)
}
