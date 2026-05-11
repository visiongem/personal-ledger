package io.github.visiongem.ledger.feature.stats

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.visiongem.ledger.core.data.domain.Account
import io.github.visiongem.ledger.core.data.domain.Category
import io.github.visiongem.ledger.core.data.domain.CategoryType
import io.github.visiongem.ledger.core.data.domain.ExchangeRate
import io.github.visiongem.ledger.core.data.domain.Record
import io.github.visiongem.ledger.core.data.domain.RecordType
import io.github.visiongem.ledger.core.data.domain.UserPreferences
import io.github.visiongem.ledger.core.data.local.prefs.UserPreferencesRepository
import io.github.visiongem.ledger.core.data.repo.AccountRepository
import io.github.visiongem.ledger.core.data.repo.CategoryRepository
import io.github.visiongem.ledger.core.data.repo.ExchangeRateRepository
import io.github.visiongem.ledger.core.data.repo.RecordRepository
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatsHomeViewModelTest {

    private val recordRepo = mockk<RecordRepository>()
    private val categoryRepo = mockk<CategoryRepository>()
    private val accountRepo = mockk<AccountRepository>()
    private val rateRepo = mockk<ExchangeRateRepository>()
    private val prefsRepo = mockk<UserPreferencesRepository>()

    private val usdAccount = Account(
        id = 1L,
        name = "USD",
        currencyCode = "USD",
        openingBalance = BigDecimal.ZERO,
        archived = false,
        createdAt = Instant.parse("2026-05-10T00:00:00Z"),
    )
    private val eurAccount = Account(
        id = 2L,
        name = "EUR",
        currencyCode = "EUR",
        openingBalance = BigDecimal.ZERO,
        archived = false,
        createdAt = Instant.parse("2026-05-10T00:00:00Z"),
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { prefsRepo.flow } returns flowOf(UserPreferences(defaultCurrency = "USD"))
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun emptyDataYieldsZeroTotals() = runTest {
        every { recordRepo.observeInRange(any(), any()) } returns flowOf(emptyList())
        every { accountRepo.observeAll() } returns flowOf(emptyList())
        every { categoryRepo.observeAll() } returns flowOf(emptyList())

        val vm = StatsHomeViewModel(recordRepo, categoryRepo, accountRepo, rateRepo, prefsRepo)

        vm.state.test {
            val loaded = awaitItem()
            assertThat(loaded.loading).isFalse()
            assertThat(loaded.incomeTotal).isEqualTo(BigDecimal.ZERO)
            assertThat(loaded.expenseTotal).isEqualTo(BigDecimal.ZERO)
            assertThat(loaded.unconvertedCount).isEqualTo(0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun aggregatesSameCurrencyIncomeAndExpense() = runTest {
        val today = LocalDate.now()
        val records = listOf(
            Record(1L, 1L, 100L, RecordType.INCOME, BigDecimal("1000"), today),
            Record(2L, 1L, 10L, RecordType.EXPENSE, BigDecimal("12.50"), today),
            Record(3L, 1L, 20L, RecordType.EXPENSE, BigDecimal("100"), today),
        )
        val categories = listOf(
            Category(10L, "Food", CategoryType.EXPENSE, null, 0),
            Category(20L, "Transport", CategoryType.EXPENSE, null, 1),
            Category(100L, "Salary", CategoryType.INCOME, null, 0),
        )
        every { recordRepo.observeInRange(any(), any()) } returns flowOf(records)
        every { accountRepo.observeAll() } returns flowOf(listOf(usdAccount))
        every { categoryRepo.observeAll() } returns flowOf(categories)

        val vm = StatsHomeViewModel(recordRepo, categoryRepo, accountRepo, rateRepo, prefsRepo)

        vm.state.test {
            val loaded = awaitItem()
            assertThat(loaded.incomeTotal).isEqualTo(BigDecimal("1000"))
            assertThat(loaded.expenseTotal).isEqualTo(BigDecimal("112.50"))
            assertThat(loaded.unconvertedCount).isEqualTo(0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun crossCurrencyConvertsViaCachedRate() = runTest {
        val today = LocalDate.now()
        val records = listOf(
            Record(1L, 2L, 10L, RecordType.EXPENSE, BigDecimal("100"), today), // EUR account
            Record(2L, 1L, 10L, RecordType.EXPENSE, BigDecimal("50"), today),  // USD account
        )
        every { recordRepo.observeInRange(any(), any()) } returns flowOf(records)
        every { accountRepo.observeAll() } returns flowOf(listOf(usdAccount, eurAccount))
        every { categoryRepo.observeAll() } returns
            flowOf(listOf(Category(10L, "Food", CategoryType.EXPENSE, null, 0)))
        every { rateRepo.observeLatest("EUR", "USD") } returns flowOf(
            ExchangeRate("EUR", "USD", BigDecimal("1.10"), today)
        )

        val vm = StatsHomeViewModel(recordRepo, categoryRepo, accountRepo, rateRepo, prefsRepo)

        vm.state.test {
            val loaded = awaitItem()
            // 100 * 1.10 = 110.00 (EUR converted) + 50 (USD already default) = 160.00
            assertThat(loaded.expenseTotal).isEqualTo(BigDecimal("160.00"))
            assertThat(loaded.unconvertedCount).isEqualTo(0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun missingRateIncrementsUnconvertedCount() = runTest {
        val today = LocalDate.now()
        val records = listOf(
            Record(1L, 2L, 10L, RecordType.EXPENSE, BigDecimal("100"), today),
        )
        every { recordRepo.observeInRange(any(), any()) } returns flowOf(records)
        every { accountRepo.observeAll() } returns flowOf(listOf(usdAccount, eurAccount))
        every { categoryRepo.observeAll() } returns
            flowOf(listOf(Category(10L, "Food", CategoryType.EXPENSE, null, 0)))
        every { rateRepo.observeLatest("EUR", "USD") } returns flowOf(null)

        val vm = StatsHomeViewModel(recordRepo, categoryRepo, accountRepo, rateRepo, prefsRepo)

        vm.state.test {
            val loaded = awaitItem()
            assertThat(loaded.expenseTotal).isEqualTo(BigDecimal.ZERO)
            assertThat(loaded.unconvertedCount).isEqualTo(1)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
