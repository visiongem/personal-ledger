package io.github.visiongem.ledger.feature.account.edit

import android.content.Context
import com.google.common.truth.Truth.assertThat
import io.github.visiongem.ledger.core.data.domain.Account
import io.github.visiongem.ledger.core.data.repo.AccountRepository
import io.mockk.coEvery
import io.mockk.coVerify
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
class AccountEditViewModelTest {

    private val context = mockk<Context>(relaxed = true).also {
        every { it.getString(any()) } returns "stub-error"
    }
    private val repo = mockk<AccountRepository>()

    private val sampleAccount = Account(
        id = 5L,
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
    fun loadIfNeededFetchesAccountAndPopulatesState() = runTest {
        every { repo.observeById(5L) } returns flowOf(sampleAccount)
        val vm = AccountEditViewModel(context, repo)

        vm.loadIfNeeded(5L)

        val state = vm.state.value
        assertThat(state.id).isEqualTo(5L)
        assertThat(state.name).isEqualTo("Cash")
        assertThat(state.currencyCode).isEqualTo("USD")
        assertThat(state.openingBalance).isEqualTo("100")
    }

    @Test
    fun loadIfNeededWithNullDoesNothing() = runTest {
        val vm = AccountEditViewModel(context, repo)
        vm.loadIfNeeded(null)
        // initial defaults retained
        assertThat(vm.state.value.id).isNull()
        assertThat(vm.state.value.name).isEqualTo("")
    }

    @Test
    fun saveWithEmptyNameSetsErrorAndDoesNotCallRepo() = runTest {
        val vm = AccountEditViewModel(context, repo)
        vm.onCurrencyChange("USD")
        vm.save()
        assertThat(vm.state.value.errorMessage).isNotNull()
        assertThat(vm.state.value.saved).isFalse()
        coVerify(exactly = 0) { repo.upsert(any()) }
    }

    @Test
    fun saveWithInvalidCurrencySetsError() = runTest {
        val vm = AccountEditViewModel(context, repo)
        vm.onNameChange("Cash")
        vm.onCurrencyChange("XX") // not 3 letters
        vm.save()
        assertThat(vm.state.value.errorMessage).isNotNull()
        coVerify(exactly = 0) { repo.upsert(any()) }
    }

    @Test
    fun saveWithInvalidBalanceSetsError() = runTest {
        val vm = AccountEditViewModel(context, repo)
        vm.onNameChange("Cash")
        vm.onOpeningBalanceChange("not-a-number")
        vm.save()
        assertThat(vm.state.value.errorMessage).isNotNull()
        coVerify(exactly = 0) { repo.upsert(any()) }
    }

    @Test
    fun saveSuccessCallsUpsertAndSetsSaved() = runTest {
        coEvery { repo.upsert(any()) } returns 9L
        val vm = AccountEditViewModel(context, repo)
        vm.onNameChange(" Cash  ") // trim should kick in
        vm.onCurrencyChange("eur")  // upper-case should kick in
        vm.onOpeningBalanceChange("250.50")

        vm.save()

        assertThat(vm.state.value.saved).isTrue()
        assertThat(vm.state.value.saving).isFalse()
        assertThat(vm.state.value.errorMessage).isNull()
        coVerify {
            repo.upsert(match {
                it.name == "Cash" &&
                    it.currencyCode == "EUR" &&
                    it.openingBalance == BigDecimal("250.50")
            })
        }
    }

    @Test
    fun fieldChangeClearsErrorMessage() = runTest {
        val vm = AccountEditViewModel(context, repo)
        vm.save() // triggers error (empty name)
        assertThat(vm.state.value.errorMessage).isNotNull()
        vm.onNameChange("Cash")
        assertThat(vm.state.value.errorMessage).isNull()
    }
}
