package io.github.visiongem.ledger.feature.settings.category

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.visiongem.ledger.core.data.domain.Category
import io.github.visiongem.ledger.core.data.domain.CategoryType
import io.github.visiongem.ledger.core.data.repo.CategoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
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
class CategoryListViewModelTest {

    private val repo = mockk<CategoryRepository>()

    private val expenseCategory = Category(
        id = 1L,
        name = "Food",
        type = CategoryType.EXPENSE,
        iconKey = null,
        sortOrder = 1,
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun splitsCategoriesByType() = runTest {
        val income = expenseCategory.copy(id = 2L, name = "Salary", type = CategoryType.INCOME)
        every { repo.observeAll() } returns flowOf(listOf(expenseCategory, income))

        val vm = CategoryListViewModel(repo)

        vm.state.test {
            val loaded = awaitItem()
            assertThat(loaded.expense.map { it.name }).containsExactly("Food")
            assertThat(loaded.income.map { it.name }).containsExactly("Salary")
            assertThat(loaded.loading).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onLongPressDeletesAndEmitsEvent() = runTest {
        every { repo.observeAll() } returns flowOf(listOf(expenseCategory))
        coEvery { repo.deleteById(any()) } returns Unit

        val vm = CategoryListViewModel(repo)

        vm.deletionEvents.test {
            vm.onLongPress(expenseCategory)
            val emitted = awaitItem()
            assertThat(emitted.id).isEqualTo(expenseCategory.id)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { repo.deleteById(expenseCategory.id) }
    }

    @Test
    fun undoDeleteUpsertsWithFreshId() = runTest {
        every { repo.observeAll() } returns flowOf(emptyList())
        val captured = slot<Category>()
        coEvery { repo.upsert(capture(captured)) } returns 99L

        val vm = CategoryListViewModel(repo)
        vm.undoDelete(expenseCategory)

        coVerify { repo.upsert(any()) }
        // id 0L lets Room autogenerate — the original row was hard-deleted.
        assertThat(captured.captured.id).isEqualTo(0L)
        assertThat(captured.captured.name).isEqualTo("Food")
        assertThat(captured.captured.type).isEqualTo(CategoryType.EXPENSE)
    }
}
