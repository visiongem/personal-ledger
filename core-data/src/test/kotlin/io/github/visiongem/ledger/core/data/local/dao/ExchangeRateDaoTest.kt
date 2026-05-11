package io.github.visiongem.ledger.core.data.local.dao

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.visiongem.ledger.core.data.local.AppDatabase
import io.github.visiongem.ledger.core.data.local.InMemoryDatabaseFactory
import io.github.visiongem.ledger.core.data.local.entity.ExchangeRateEntity
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExchangeRateDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ExchangeRateDao

    @Before
    fun setUp() {
        db = InMemoryDatabaseFactory.create()
        dao = db.exchangeRateDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun upsertAllStoresEachRateAndGetRateFindsBy3KeyComposite() = runTest {
        val asOf = LocalDate.of(2026, 5, 10)
        dao.upsertAll(
            listOf(
                ExchangeRateEntity("USD", "CNY", BigDecimal("7.23"), asOf),
                ExchangeRateEntity("USD", "EUR", BigDecimal("0.92"), asOf),
            )
        )
        assertThat(dao.getRate("USD", "CNY", asOf)?.rate).isEqualTo(BigDecimal("7.23"))
        assertThat(dao.getRate("USD", "EUR", asOf)?.rate).isEqualTo(BigDecimal("0.92"))
        assertThat(dao.getRate("USD", "JPY", asOf)).isNull()
    }

    @Test
    fun observeLatestReturnsMostRecentAsOf() = runTest {
        dao.upsertAll(
            listOf(
                ExchangeRateEntity("USD", "CNY", BigDecimal("7.10"), LocalDate.of(2026, 5, 9)),
                ExchangeRateEntity("USD", "CNY", BigDecimal("7.23"), LocalDate.of(2026, 5, 11)),
                ExchangeRateEntity("USD", "CNY", BigDecimal("7.15"), LocalDate.of(2026, 5, 10)),
            )
        )

        dao.observeLatest("USD", "CNY").test {
            val latest = awaitItem()!!
            assertThat(latest.rate).isEqualTo(BigDecimal("7.23"))
            assertThat(latest.asOf).isEqualTo(LocalDate.of(2026, 5, 11))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun upsertReplacesSameCompositeKey() = runTest {
        val asOf = LocalDate.of(2026, 5, 10)
        dao.upsertAll(listOf(ExchangeRateEntity("USD", "CNY", BigDecimal("7.10"), asOf)))
        dao.upsertAll(listOf(ExchangeRateEntity("USD", "CNY", BigDecimal("7.23"), asOf)))

        assertThat(dao.getRate("USD", "CNY", asOf)?.rate).isEqualTo(BigDecimal("7.23"))
    }

    @Test
    fun deleteBeforePrunesEarlierAsOf() = runTest {
        dao.upsertAll(
            listOf(
                ExchangeRateEntity("USD", "CNY", BigDecimal("7.10"), LocalDate.of(2026, 5, 1)),
                ExchangeRateEntity("USD", "CNY", BigDecimal("7.20"), LocalDate.of(2026, 5, 10)),
            )
        )

        dao.deleteBefore(LocalDate.of(2026, 5, 5))

        assertThat(dao.getRate("USD", "CNY", LocalDate.of(2026, 5, 1))).isNull()
        assertThat(dao.getRate("USD", "CNY", LocalDate.of(2026, 5, 10))?.rate)
            .isEqualTo(BigDecimal("7.20"))
    }
}
