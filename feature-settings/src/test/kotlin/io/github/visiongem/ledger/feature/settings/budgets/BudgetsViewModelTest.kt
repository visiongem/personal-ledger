package io.github.visiongem.ledger.feature.settings.budgets

import com.google.common.truth.Truth.assertThat
import io.github.visiongem.ledger.core.data.domain.Budget
import io.github.visiongem.ledger.core.data.domain.UserPreferences
import io.github.visiongem.ledger.core.data.local.prefs.UserPreferencesRepository
import io.github.visiongem.ledger.core.data.repo.BudgetRepository
import io.github.visiongem.ledger.core.data.repo.CategoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.math.BigDecimal
import java.time.YearMonth
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
class BudgetsViewModelTest {

    private val budgetRepo = mockk<BudgetRepository>()
    private val categoryRepo = mockk<CategoryRepository>()
    private val prefsRepo = mockk<UserPreferencesRepository>()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun stubObserves() {
        every { categoryRepo.observeByType(any()) } returns flowOf(emptyList())
        every { budgetRepo.observeBudgetsWithUsage(any()) } returns flowOf(emptyList())
        every { prefsRepo.flow } returns flowOf(UserPreferences(defaultCurrency = "USD"))
    }

    @Test
    fun setBudgetInsertsWithCurrentDefaultCurrency() = runTest {
        stubObserves()
        every { budgetRepo.observeByCategoryAndMonth(any(), any()) } returns flowOf(null)
        val captured = slot<Budget>()
        coEvery { budgetRepo.upsert(capture(captured)) } returns 1L

        val vm = BudgetsViewModel(budgetRepo, categoryRepo, prefsRepo)
        vm.setBudget(categoryId = 7L, amount = BigDecimal("500"))

        coVerify { budgetRepo.upsert(any()) }
        assertThat(captured.captured.categoryId).isEqualTo(7L)
        assertThat(captured.captured.limit).isEqualTo(BigDecimal("500"))
        assertThat(captured.captured.currencyCode).isEqualTo("USD")
        assertThat(captured.captured.month).isEqualTo(YearMonth.now())
        // id = 0 because the prior observe returned null — Room autogenerates.
        assertThat(captured.captured.id).isEqualTo(0L)
    }

    @Test
    fun setBudgetUpdatesPreservesExistingId() = runTest {
        stubObserves()
        val existing = Budget(
            id = 42L,
            categoryId = 7L,
            month = YearMonth.now(),
            limit = BigDecimal("300"),
            currencyCode = "USD",
        )
        every { budgetRepo.observeByCategoryAndMonth(7L, any()) } returns flowOf(existing)
        val captured = slot<Budget>()
        coEvery { budgetRepo.upsert(capture(captured)) } returns 42L

        val vm = BudgetsViewModel(budgetRepo, categoryRepo, prefsRepo)
        vm.setBudget(categoryId = 7L, amount = BigDecimal("800"))

        assertThat(captured.captured.id).isEqualTo(42L)
        assertThat(captured.captured.limit).isEqualTo(BigDecimal("800"))
    }

    @Test
    fun clearBudgetDeletesExistingRow() = runTest {
        stubObserves()
        val existing = Budget(
            id = 42L,
            categoryId = 7L,
            month = YearMonth.now(),
            limit = BigDecimal("300"),
            currencyCode = "USD",
        )
        every { budgetRepo.observeByCategoryAndMonth(7L, any()) } returns flowOf(existing)
        coEvery { budgetRepo.deleteById(42L) } returns Unit

        val vm = BudgetsViewModel(budgetRepo, categoryRepo, prefsRepo)
        vm.clearBudget(categoryId = 7L)

        coVerify { budgetRepo.deleteById(42L) }
    }

    @Test
    fun clearBudgetNoOpWhenNoRow() = runTest {
        stubObserves()
        every { budgetRepo.observeByCategoryAndMonth(any(), any()) } returns flowOf(null)

        val vm = BudgetsViewModel(budgetRepo, categoryRepo, prefsRepo)
        vm.clearBudget(categoryId = 999L)

        coVerify(exactly = 0) { budgetRepo.deleteById(any()) }
    }
}
