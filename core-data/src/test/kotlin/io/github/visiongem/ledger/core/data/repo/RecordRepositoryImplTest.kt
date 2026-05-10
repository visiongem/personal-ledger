package io.github.visiongem.ledger.core.data.repo

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.visiongem.ledger.core.data.domain.Record
import io.github.visiongem.ledger.core.data.domain.RecordType
import io.github.visiongem.ledger.core.data.local.dao.RecordDao
import io.github.visiongem.ledger.core.data.local.entity.RecordEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class RecordRepositoryImplTest {

    private val dao = mockk<RecordDao>()
    private val repo: RecordRepository = RecordRepositoryImpl(dao)

    private val today = LocalDate.of(2026, 5, 10)

    private fun entity(
        id: Long,
        categoryId: Long?,
        type: RecordType,
        amount: String,
    ) = RecordEntity(
        id = id,
        accountId = 1L,
        categoryId = categoryId,
        type = type,
        amount = BigDecimal(amount),
        occurredOn = today,
        note = null,
        transferToAccountId = null,
        transferAmount = null,
    )

    @Test
    fun observeInRangeMapsToDomain() = runTest {
        every { dao.observeInRange(today, today) } returns
            flowOf(listOf(entity(1L, 10L, RecordType.EXPENSE, "5.00")))
        repo.observeInRange(today, today).test {
            val list = awaitItem()
            assertThat(list).hasSize(1)
            assertThat(list[0].amount).isEqualTo(BigDecimal("5.00"))
            awaitComplete()
        }
    }

    @Test
    fun aggregatesByCategoryAndIgnoresOtherTypesAndNullCategory() = runTest {
        val records = listOf(
            entity(1L, 10L, RecordType.EXPENSE, "10.50"),
            entity(2L, 10L, RecordType.EXPENSE, "5.50"),
            entity(3L, 20L, RecordType.EXPENSE, "100.00"),
            entity(4L, 10L, RecordType.INCOME, "1000.00"), // wrong type
            entity(5L, null, RecordType.EXPENSE, "99.00"), // null category
        )
        every { dao.observeInRange(today, today) } returns flowOf(records)

        repo.observeCategoryTotalsInRange(RecordType.EXPENSE, today, today).test {
            val totals = awaitItem()
            assertThat(totals).containsExactly(
                10L, BigDecimal("16.00"),
                20L, BigDecimal("100.00"),
            )
            awaitComplete()
        }
    }

    @Test
    fun aggregatesEmptyOnEmptyInput() = runTest {
        every { dao.observeInRange(today, today) } returns flowOf(emptyList())
        repo.observeCategoryTotalsInRange(RecordType.EXPENSE, today, today).test {
            assertThat(awaitItem()).isEmpty()
            awaitComplete()
        }
    }

    @Test
    fun upsertConvertsDomainToEntity() = runTest {
        coEvery { dao.upsert(any()) } returns 99L
        val record = Record(
            id = 0L,
            accountId = 1L,
            categoryId = 10L,
            type = RecordType.EXPENSE,
            amount = BigDecimal("12.34"),
            occurredOn = today,
            note = "lunch",
        )
        assertThat(repo.upsert(record)).isEqualTo(99L)
        coVerify { dao.upsert(match { it.amount == BigDecimal("12.34") && it.note == "lunch" }) }
    }

    @Test
    fun deleteByIdDelegates() = runTest {
        coEvery { dao.deleteById(any()) } returns Unit
        repo.deleteById(7L)
        coVerify { dao.deleteById(7L) }
    }
}
