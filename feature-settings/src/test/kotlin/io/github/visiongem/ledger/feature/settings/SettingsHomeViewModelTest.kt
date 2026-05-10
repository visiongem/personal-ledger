package io.github.visiongem.ledger.feature.settings

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.visiongem.ledger.core.data.domain.ThemeMode
import io.github.visiongem.ledger.core.data.domain.UserPreferences
import io.github.visiongem.ledger.core.data.local.prefs.UserPreferencesRepository
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
class SettingsHomeViewModelTest {

    private val prefsRepo = mockk<UserPreferencesRepository>()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun stateMirrorsRepositoryFlow() = runTest {
        every { prefsRepo.flow } returns flowOf(
            UserPreferences(themeMode = ThemeMode.DARK, defaultCurrency = "EUR")
        )

        val vm = SettingsHomeViewModel(prefsRepo)

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

        val vm = SettingsHomeViewModel(prefsRepo)
        vm.onThemeModeChange(ThemeMode.LIGHT)
        coVerify { prefsRepo.setThemeMode(ThemeMode.LIGHT) }
    }

    @Test
    fun onDefaultCurrencyChangeNormalizesAndDelegates() = runTest {
        every { prefsRepo.flow } returns flowOf(UserPreferences())
        coEvery { prefsRepo.setDefaultCurrency(any()) } returns Unit

        val vm = SettingsHomeViewModel(prefsRepo)
        vm.onDefaultCurrencyChange("eur") // lowercase + 3 letters
        coVerify { prefsRepo.setDefaultCurrency("EUR") }
    }

    @Test
    fun onDefaultCurrencyChangeTruncatesToThreeLetters() = runTest {
        every { prefsRepo.flow } returns flowOf(UserPreferences())
        coEvery { prefsRepo.setDefaultCurrency(any()) } returns Unit

        val vm = SettingsHomeViewModel(prefsRepo)
        vm.onDefaultCurrencyChange("euros") // 5 letters
        coVerify { prefsRepo.setDefaultCurrency("EUR") }
    }
}
