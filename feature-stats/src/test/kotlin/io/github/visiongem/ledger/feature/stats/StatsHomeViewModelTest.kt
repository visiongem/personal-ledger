package io.github.visiongem.ledger.feature.stats

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.visiongem.ledger.core.data.domain.Category
import io.github.visiongem.ledger.core.data.domain.CategoryType
import io.github.visiongem.ledger.core.data.domain.Record
import io.github.visiongem.ledger.core.data.domain.RecordType
import io.github.visiongem.ledger.core.data.domain.UserPreferences
import io.github.visiongem.ledger.core.data.local.prefs.UserPreferencesRepository
import io.github.visiongem.ledger.core.data.repo.CategoryRepository
import io.github.visiongem.ledger.core.data.repo.RecordRepository
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
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
    private val prefsRepo = mockk<UserPreferencesRepository>()

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
        every { categoryRepo.observeAll() } returns flowOf(emptyList())

        val vm = StatsHomeViewModel(recordRepo, categoryRepo, prefsRepo)

        vm.state.test {
            val loaded = awaitItem()
            assertThat(loaded.loading).isFalse()
            assertThat(loaded.incomeTotal).isEqualTo(BigDecimal.ZERO)
            assertThat(loaded.expenseTotal).isEqualTo(BigDecimal.ZERO)
            assertThat(loaded.net).isEqualTo(BigDecimal.ZERO)
            assertThat(loaded.expenseByCategory).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun aggregatesIncomeExpenseAndCategoryBreakdown() = runTest {
        val today = LocalDate.now()
        val records = listOf(
            Record(1L, 1L, 100L, RecordType.INCOME, BigDecimal("1000"), today),
            Record(2L, 1L, 10L, RecordType.EXPENSE, BigDecimal("12.50"), today),
            Record(3L, 1L, 10L, RecordType.EXPENSE, BigDecimal("7.50"), today),
            Record(4L, 1L, 20L, RecordType.EXPENSE, BigDecimal("100"), today),
        )
        val categories = listOf(
            Category(10L, "Food", CategoryType.EXPENSE, null, 0),
            Category(20L, "Transport", CategoryType.EXPENSE, null, 1),
            Category(100L, "Salary", CategoryType.INCOME, null, 0),
        )
        every { recordRepo.observeInRange(any(), any()) } returns flowOf(records)
        every { categoryRepo.observeAll() } returns flowOf(categories)

        val vm = StatsHomeViewModel(recordRepo, categoryRepo, prefsRepo)

        vm.state.test {
            val loaded = awaitItem()
            assertThat(loaded.incomeTotal).isEqualTo(BigDecimal("1000"))
            assertThat(loaded.expenseTotal).isEqualTo(BigDecimal("120.00"))
            assertThat(loaded.net).isEqualTo(BigDecimal("880.00"))
            // Sorted by total desc — Transport (100) > Food (20.00)
            assertThat(loaded.expenseByCategory).hasSize(2)
            assertThat(loaded.expenseByCategory[0].categoryName).isEqualTo("Transport")
            assertThat(loaded.expenseByCategory[0].total).isEqualTo(BigDecimal("100"))
            assertThat(loaded.expenseByCategory[1].categoryName).isEqualTo("Food")
            assertThat(loaded.expenseByCategory[1].total).isEqualTo(BigDecimal("20.00"))
            cancelAndIgnoreRemainingEvents()
        }
    }
}
