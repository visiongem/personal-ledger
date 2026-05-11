package io.github.visiongem.ledger.core.data.repo

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.visiongem.ledger.core.data.local.dao.ExchangeRateDao
import io.github.visiongem.ledger.core.data.local.entity.ExchangeRateEntity
import io.github.visiongem.ledger.core.data.local.prefs.UserPreferencesRepository
import io.github.visiongem.ledger.core.data.remote.FrankfurterApi
import io.github.visiongem.ledger.core.data.remote.FrankfurterRatesResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ExchangeRateRepositoryImplTest {

    private val dao = mockk<ExchangeRateDao>()
    private val api = mockk<FrankfurterApi>()
    private val userPreferencesRepository = mockk<UserPreferencesRepository>(relaxed = true)
    private val repo: ExchangeRateRepository = ExchangeRateRepositoryImpl(dao, api, userPreferencesRepository)

    private val date = LocalDate.of(2026, 5, 10)

    @Test
    fun observeLatestMapsToDomain() = runTest {
        every { dao.observeLatest("USD", "CNY") } returns flowOf(
            ExchangeRateEntity("USD", "CNY", BigDecimal("7.23"), date)
        )
        repo.observeLatest("USD", "CNY").test {
            val rate = awaitItem()
            assertThat(rate).isNotNull()
            assertThat(rate!!.rate).isEqualTo(BigDecimal("7.23"))
            awaitComplete()
        }
    }

    @Test
    fun getRateOnDateCacheHitSkipsApi() = runTest {
        coEvery { dao.getRate("USD", "CNY", date) } returns
            ExchangeRateEntity("USD", "CNY", BigDecimal("7.23"), date)

        val result = repo.getRateOnDate("USD", "CNY", date)

        assertThat(result?.rate).isEqualTo(BigDecimal("7.23"))
        coVerify(exactly = 0) { api.getOnDate(any(), any(), any()) }
    }

    @Test
    fun getRateOnDateCacheMissFetchesAndCaches() = runTest {
        coEvery { dao.getRate("USD", "CNY", date) } returns null
        coEvery { api.getOnDate("2026-05-10", "USD", "CNY") } returns FrankfurterRatesResponse(
            amount = 1.0,
            base = "USD",
            date = "2026-05-10",
            rates = mapOf("CNY" to 7.23),
        )
        coEvery { dao.upsertAll(any()) } returns Unit

        val result = repo.getRateOnDate("USD", "CNY", date)

        assertThat(result).isNotNull()
        assertThat(result!!.rate).isEqualTo(BigDecimal("7.23"))
        coVerify {
            dao.upsertAll(match { entities ->
                entities.size == 1 &&
                    entities[0].quoteCurrency == "CNY" &&
                    entities[0].rate == BigDecimal("7.23")
            })
        }
    }

    @Test
    fun getRateOnDateApiMissingQuoteReturnsNull() = runTest {
        coEvery { dao.getRate("USD", "JPY", date) } returns null
        coEvery { api.getOnDate("2026-05-10", "USD", "JPY") } returns FrankfurterRatesResponse(
            amount = 1.0,
            base = "USD",
            date = "2026-05-10",
            rates = emptyMap(),
        )

        assertThat(repo.getRateOnDate("USD", "JPY", date)).isNull()
    }

    @Test
    fun refreshLatestReturnsSuccessAndCaches() = runTest {
        coEvery { api.getLatest("USD", "CNY,EUR") } returns FrankfurterRatesResponse(
            amount = 1.0,
            base = "USD",
            date = "2026-05-10",
            rates = mapOf("CNY" to 7.23, "EUR" to 0.92),
        )
        coEvery { dao.upsertAll(any()) } returns Unit

        val result = repo.refreshLatest("USD", listOf("CNY", "EUR"))

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow()).hasSize(2)
        coVerify { dao.upsertAll(match { it.size == 2 }) }
        coVerify { userPreferencesRepository.setLastRateRefreshAt(any<Instant>()) }
    }

    @Test
    fun refreshLatestOnApiErrorReturnsFailure() = runTest {
        coEvery { api.getLatest(any(), any()) } throws IOException("network down")

        val result = repo.refreshLatest("USD", listOf("CNY"))

        assertThat(result.isFailure).isTrue()
        coVerify(exactly = 0) { dao.upsertAll(any()) }
        coVerify(exactly = 0) { userPreferencesRepository.setLastRateRefreshAt(any<Instant>()) }
    }
}
