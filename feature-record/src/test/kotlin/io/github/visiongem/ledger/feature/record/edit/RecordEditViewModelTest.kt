package io.github.visiongem.ledger.feature.record.edit

import com.google.common.truth.Truth.assertThat
import io.github.visiongem.ledger.core.data.domain.Category
import io.github.visiongem.ledger.core.data.domain.CategoryType
import io.github.visiongem.ledger.core.data.domain.RecordType
import io.github.visiongem.ledger.core.data.repo.AccountRepository
import io.github.visiongem.ledger.core.data.repo.CategoryRepository
import io.github.visiongem.ledger.core.data.repo.RecordRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
class RecordEditViewModelTest {

    private val recordRepo = mockk<RecordRepository>(relaxed = true)
    private val accountRepo = mockk<AccountRepository>()
    private val categoryRepo = mockk<CategoryRepository>()

    private val incomeCategory = Category(1L, "Salary", CategoryType.INCOME, null, 0)
    private val expenseCategory = Category(2L, "Food", CategoryType.EXPENSE, null, 0)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { accountRepo.observeActive() } returns flowOf(emptyList())
        every { categoryRepo.observeAll() } returns
            flowOf(listOf(incomeCategory, expenseCategory))
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun categoryOptionsFilterByType() = runTest {
        val vm = RecordEditViewModel(recordRepo, accountRepo, categoryRepo)
        // Default type is EXPENSE — only expense category should appear
        assertThat(vm.state.value.categoryOptions).containsExactly(expenseCategory)
    }

    @Test
    fun typeChangeClearsCategoryAndRefilters() = runTest {
        val vm = RecordEditViewModel(recordRepo, accountRepo, categoryRepo)
        vm.onCategoryChange(2L)
        assertThat(vm.state.value.categoryId).isEqualTo(2L)
        vm.onTypeChange(RecordType.INCOME)
        assertThat(vm.state.value.categoryId).isNull()
        assertThat(vm.state.value.categoryOptions).containsExactly(incomeCategory)
    }

    @Test
    fun saveWithoutAccountSetsError() = runTest {
        val vm = RecordEditViewModel(recordRepo, accountRepo, categoryRepo)
        vm.onCategoryChange(2L)
        vm.onAmountChange("10")
        vm.save()
        assertThat(vm.state.value.errorMessage).contains("account")
        coVerify(exactly = 0) { recordRepo.upsert(any()) }
    }

    @Test
    fun saveWithInvalidAmountSetsError() = runTest {
        val vm = RecordEditViewModel(recordRepo, accountRepo, categoryRepo)
        vm.onAccountChange(1L)
        vm.onCategoryChange(2L)
        vm.onAmountChange("-5")
        vm.save()
        assertThat(vm.state.value.errorMessage).contains("positive")
    }

    @Test
    fun saveWithInvalidDateSetsError() = runTest {
        val vm = RecordEditViewModel(recordRepo, accountRepo, categoryRepo)
        vm.onAccountChange(1L)
        vm.onCategoryChange(2L)
        vm.onAmountChange("10")
        vm.onDateChange("not-a-date")
        vm.save()
        assertThat(vm.state.value.errorMessage).contains("Date")
    }

    @Test
    fun saveSuccessUpsertsAndSetsSaved() = runTest {
        coEvery { recordRepo.upsert(any()) } returns 9L
        val vm = RecordEditViewModel(recordRepo, accountRepo, categoryRepo)
        vm.onAccountChange(1L)
        vm.onCategoryChange(2L)
        vm.onAmountChange("12.34")
        vm.onDateChange("2026-05-10")
        vm.onNoteChange("Lunch")
        vm.save()
        assertThat(vm.state.value.saved).isTrue()
        coVerify {
            recordRepo.upsert(match {
                it.accountId == 1L &&
                    it.categoryId == 2L &&
                    it.amount.toPlainString() == "12.34" &&
                    it.note == "Lunch"
            })
        }
    }
}
