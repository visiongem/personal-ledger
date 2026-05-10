package io.github.visiongem.ledger.feature.record.list

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.visiongem.ledger.core.data.domain.Account
import io.github.visiongem.ledger.core.data.domain.Category
import io.github.visiongem.ledger.core.data.domain.CategoryType
import io.github.visiongem.ledger.core.data.domain.Record
import io.github.visiongem.ledger.core.data.domain.RecordType
import io.github.visiongem.ledger.core.data.repo.AccountRepository
import io.github.visiongem.ledger.core.data.repo.CategoryRepository
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
class RecordListViewModelTest {

    private val recordRepo = mockk<RecordRepository>()
    private val accountRepo = mockk<AccountRepository>()
    private val categoryRepo = mockk<CategoryRepository>()

    private val cashAccount = Account(
        id = 1L,
        name = "Cash",
        currencyCode = "USD",
        openingBalance = BigDecimal("0"),
        archived = false,
        createdAt = Instant.parse("2026-05-10T00:00:00Z"),
    )
    private val foodCategory = Category(
        id = 10L,
        name = "Food",
        type = CategoryType.EXPENSE,
        iconKey = null,
        sortOrder = 0,
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
    fun emptyDataYieldsEmptyRowsAndLoadingFalse() = runTest {
        every { recordRepo.observeInRange(any(), any()) } returns flowOf(emptyList())
        every { accountRepo.observeAll() } returns flowOf(emptyList())
        every { categoryRepo.observeAll() } returns flowOf(emptyList())

        val vm = RecordListViewModel(recordRepo, accountRepo, categoryRepo)

        vm.state.test {
            val loaded = awaitItem()
            assertThat(loaded.loading).isFalse()
            assertThat(loaded.rows).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun composesRowsWithAccountAndCategoryNames() = runTest {
        val record = Record(
            id = 1L,
            accountId = 1L,
            categoryId = 10L,
            type = RecordType.EXPENSE,
            amount = BigDecimal("12.50"),
            occurredOn = LocalDate.of(2026, 5, 10),
            note = "lunch",
        )
        every { recordRepo.observeInRange(any(), any()) } returns flowOf(listOf(record))
        every { accountRepo.observeAll() } returns flowOf(listOf(cashAccount))
        every { categoryRepo.observeAll() } returns flowOf(listOf(foodCategory))

        val vm = RecordListViewModel(recordRepo, accountRepo, categoryRepo)

        vm.state.test {
            val loaded = awaitItem()
            assertThat(loaded.rows).hasSize(1)
            val row = loaded.rows[0]
            assertThat(row.accountName).isEqualTo("Cash")
            assertThat(row.categoryName).isEqualTo("Food")
            assertThat(row.displayCurrency).isEqualTo("USD")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun unknownAccountFallsBack() = runTest {
        val record = Record(
            id = 2L,
            accountId = 999L, // not in accounts list
            categoryId = null,
            type = RecordType.INCOME,
            amount = BigDecimal("1000"),
            occurredOn = LocalDate.of(2026, 5, 1),
            note = null,
        )
        every { recordRepo.observeInRange(any(), any()) } returns flowOf(listOf(record))
        every { accountRepo.observeAll() } returns flowOf(emptyList())
        every { categoryRepo.observeAll() } returns flowOf(emptyList())

        val vm = RecordListViewModel(recordRepo, accountRepo, categoryRepo)

        vm.state.test {
            val loaded = awaitItem()
            val row = loaded.rows[0]
            assertThat(row.accountName).isEqualTo("Unknown")
            assertThat(row.categoryName).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
