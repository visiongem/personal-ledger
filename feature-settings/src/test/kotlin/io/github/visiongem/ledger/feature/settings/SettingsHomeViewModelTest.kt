package io.github.visiongem.ledger.feature.settings

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.visiongem.ledger.core.data.domain.Account
import io.github.visiongem.ledger.core.data.domain.ExchangeRate
import io.github.visiongem.ledger.core.data.domain.ThemeMode
import io.github.visiongem.ledger.core.data.domain.UserPreferences
import io.github.visiongem.ledger.core.data.local.prefs.UserPreferencesRepository
import android.content.Context
import io.github.visiongem.ledger.core.data.backup.RecordBackupRepository
import io.github.visiongem.ledger.core.data.repo.AccountRepository
import io.github.visiongem.ledger.core.data.repo.ExchangeRateRepository
import io.mockk.coEvery
import io.mockk.coVerify
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
class SettingsHomeViewModelTest {

    private val context = mockk<Context>(relaxed = true)
    private val prefsRepo = mockk<UserPreferencesRepository>()
    private val accountRepo = mockk<AccountRepository>()
    private val rateRepo = mockk<ExchangeRateRepository>()
    private val backupRepo = mockk<RecordBackupRepository>(relaxed = true)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newVm() = SettingsHomeViewModel(context, prefsRepo, accountRepo, rateRepo, backupRepo)

    @Test
    fun stateMirrorsRepositoryFlow() = runTest {
        every { prefsRepo.flow } returns flowOf(
            UserPreferences(themeMode = ThemeMode.DARK, defaultCurrency = "EUR")
        )

        val vm = newVm()

        vm.state.test {
            val ui = awaitItem()
            assertThat(ui.themeMode).isEqualTo(ThemeMode.DARK)
            assertThat(ui.defaultCurrency).isEqualTo("EUR")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onThemeModeChangeDelegatesToRepo() = runTest {
        every { prefsRepo.flow } returns flowOf(UserPreferences())
        coEvery { prefsRepo.setThemeMode(any()) } returns Unit

        val vm = newVm()
        vm.onThemeModeChange(ThemeMode.LIGHT)
        coVerify { prefsRepo.setThemeMode(ThemeMode.LIGHT) }
    }

    @Test
    fun onDefaultCurrencyChangeNormalizesAndDelegates() = runTest {
        every { prefsRepo.flow } returns flowOf(UserPreferences())
        coEvery { prefsRepo.setDefaultCurrency(any()) } returns Unit

        val vm = newVm()
        vm.onDefaultCurrencyChange("eur")
        coVerify { prefsRepo.setDefaultCurrency("EUR") }
    }

    @Test
    fun onDefaultCurrencyChangeTruncatesToThreeLetters() = runTest {
        every { prefsRepo.flow } returns flowOf(UserPreferences())
        coEvery { prefsRepo.setDefaultCurrency(any()) } returns Unit

        val vm = newVm()
        vm.onDefaultCurrencyChange("euros")
        coVerify { prefsRepo.setDefaultCurrency("EUR") }
    }

    @Test
    fun onDefaultCurrencyChangeRejectsShortInput() = runTest {
        every { prefsRepo.flow } returns flowOf(UserPreferences())

        val vm = newVm()
        vm.onDefaultCurrencyChange("eu") // only 2 letters
        vm.onDefaultCurrencyChange("")    // empty
        vm.onDefaultCurrencyChange("u 1") // non-letters dropped, leaves only "U"
        coVerify(exactly = 0) { prefsRepo.setDefaultCurrency(any()) }
    }

    @Test
    fun onDefaultCurrencyChangeDropsDigitsAndPunctuation() = runTest {
        every { prefsRepo.flow } returns flowOf(UserPreferences())
        coEvery { prefsRepo.setDefaultCurrency(any()) } returns Unit

        val vm = newVm()
        vm.onDefaultCurrencyChange("u-s-d-x") // letters: u, s, d, x → take 3 → USD
        coVerify { prefsRepo.setDefaultCurrency("USD") }
    }

    @Test
    fun refreshRatesCallsRepoWithOtherCurrencies() = runTest {
        every { prefsRepo.flow } returns flowOf(UserPreferences(defaultCurrency = "USD"))
        every { accountRepo.observeAll() } returns flowOf(
            listOf(
                Account(1L, "USD", "USD", BigDecimal.ZERO, false, Instant.parse("2026-05-10T00:00:00Z")),
                Account(2L, "EUR", "EUR", BigDecimal.ZERO, false, Instant.parse("2026-05-10T00:00:00Z")),
                Account(3L, "CNY", "CNY", BigDecimal.ZERO, false, Instant.parse("2026-05-10T00:00:00Z")),
            )
        )
        coEvery { rateRepo.refreshLatest("USD", listOf("EUR", "CNY")) } returns
            Result.success(emptyList<ExchangeRate>())

        val vm = newVm()
        vm.refreshRates()

        coVerify { rateRepo.refreshLatest("USD", listOf("EUR", "CNY")) }
    }

    @Test
    fun refreshRatesNoOpWhenAllAccountsMatchDefault() = runTest {
        every { prefsRepo.flow } returns flowOf(UserPreferences(defaultCurrency = "USD"))
        every { accountRepo.observeAll() } returns flowOf(
            listOf(
                Account(1L, "USD", "USD", BigDecimal.ZERO, false, Instant.parse("2026-05-10T00:00:00Z")),
            )
        )

        val vm = newVm()
        vm.refreshRates()

        coVerify(exactly = 0) { rateRepo.refreshLatest(any(), any()) }
    }
}
