package io.github.visiongem.ledger.feature.account.list

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.visiongem.ledger.core.data.domain.Account
import io.github.visiongem.ledger.core.data.repo.AccountRepository
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import java.time.Instant
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
class AccountListViewModelTest {

    private val repo = mockk<AccountRepository>()

    private val sampleAccount = Account(
        id = 1L,
        name = "Cash",
        currencyCode = "USD",
        openingBalance = BigDecimal("100"),
        archived = false,
        createdAt = Instant.parse("2026-05-10T10:00:00Z"),
    )

    @BeforeEach
    fun setMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateIsLoadingWithEmptyAccounts() {
        every { repo.observeActive() } returns flowOf(emptyList())
        val vm = AccountListViewModel(repo)
        assertThat(vm.state.value.loading).isTrue()
        assertThat(vm.state.value.accounts).isEmpty()
    }

    @Test
    fun loadingFlipsToFalseAfterFirstEmit() = runTest {
        every { repo.observeActive() } returns flowOf(listOf(sampleAccount))
        val vm = AccountListViewModel(repo)

        vm.state.test {
            val loaded = awaitItem()
            assertThat(loaded.loading).isFalse()
            assertThat(loaded.accounts).hasSize(1)
            assertThat(loaded.accounts[0].name).isEqualTo("Cash")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun emptyListEmitFlipsLoadingToFalse() = runTest {
        every { repo.observeActive() } returns flowOf(emptyList())
        val vm = AccountListViewModel(repo)

        vm.state.test {
            val loaded = awaitItem()
            assertThat(loaded.loading).isFalse()
            assertThat(loaded.accounts).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
