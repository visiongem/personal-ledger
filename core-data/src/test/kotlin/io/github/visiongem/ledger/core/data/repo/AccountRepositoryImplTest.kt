package io.github.visiongem.ledger.core.data.repo

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.visiongem.ledger.core.data.domain.Account
import io.github.visiongem.ledger.core.data.local.dao.AccountDao
import io.github.visiongem.ledger.core.data.local.entity.AccountEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class AccountRepositoryImplTest {

    private val dao = mockk<AccountDao>()
    private val repo: AccountRepository = AccountRepositoryImpl(dao)

    private val sampleEntity = AccountEntity(
        id = 1L,
        name = "Cash",
        currencyCode = "USD",
        openingBalance = BigDecimal("100.00"),
        archived = false,
        createdAt = Instant.parse("2026-05-10T10:00:00Z"),
    )

    @Test
    fun observeAllMapsEntitiesToDomain() = runTest {
        every { dao.observeAll() } returns flowOf(listOf(sampleEntity))

        repo.observeAll().test {
            val received = awaitItem()
            assertThat(received).hasSize(1)
            assertThat(received[0].name).isEqualTo("Cash")
            assertThat(received[0].currencyCode).isEqualTo("USD")
            awaitComplete()
        }
    }

    @Test
    fun observeActiveDelegatesAndMaps() = runTest {
        every { dao.observeActive() } returns flowOf(listOf(sampleEntity))
        repo.observeActive().test {
            assertThat(awaitItem()).hasSize(1)
            awaitComplete()
        }
    }

    @Test
    fun observeByIdMapsNullable() = runTest {
        every { dao.observeById(99L) } returns flowOf(null)
        repo.observeById(99L).test {
            assertThat(awaitItem()).isNull()
            awaitComplete()
        }
    }

    @Test
    fun upsertConvertsDomainToEntity() = runTest {
        coEvery { dao.upsert(any()) } returns 7L
        val account = Account(
            id = 0L,
            name = "New",
            currencyCode = "EUR",
            openingBalance = BigDecimal("50.00"),
            archived = false,
            createdAt = Instant.parse("2026-05-10T10:00:00Z"),
        )
        val newId = repo.upsert(account)
        assertThat(newId).isEqualTo(7L)
        coVerify { dao.upsert(match { it.name == "New" && it.currencyCode == "EUR" }) }
    }

    @Test
    fun deleteByIdDelegatesToDao() = runTest {
        coEvery { dao.deleteById(any()) } returns Unit
        repo.deleteById(42L)
        coVerify { dao.deleteById(42L) }
    }
}
