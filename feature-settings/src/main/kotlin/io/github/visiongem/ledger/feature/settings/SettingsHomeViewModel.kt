package io.github.visiongem.ledger.feature.settings

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.visiongem.ledger.core.base.BaseViewModel
import io.github.visiongem.ledger.core.data.domain.ThemeMode
import io.github.visiongem.ledger.core.data.local.prefs.UserPreferencesRepository
import io.github.visiongem.ledger.core.data.repo.AccountRepository
import io.github.visiongem.ledger.core.data.repo.ExchangeRateRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsHomeViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val accountRepository: AccountRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
) : BaseViewModel() {

    private val transientState = MutableStateFlow(TransientSettingsState())

    val state: StateFlow<SettingsHomeUiState> = combine(
        userPreferencesRepository.flow,
        transientState,
    ) { prefs, transient ->
        SettingsHomeUiState(
            themeMode = prefs.themeMode,
            defaultCurrency = prefs.defaultCurrency,
            refreshingRates = transient.refreshingRates,
            ratesMessage = transient.ratesMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STATE_TIMEOUT_MILLIS),
        initialValue = SettingsHomeUiState(),
    )

    fun onThemeModeChange(mode: ThemeMode) {
        viewModelScope.launch {
            userPreferencesRepository.setThemeMode(mode)
        }
    }

    fun onDefaultCurrencyChange(currency: String) {
        val normalized = currency.uppercase().take(MAX_CURRENCY_LENGTH)
        viewModelScope.launch {
            userPreferencesRepository.setDefaultCurrency(normalized)
        }
    }

    fun refreshRates() {
        viewModelScope.launch {
            val defaultCurrency = userPreferencesRepository.flow.first().defaultCurrency
            val others = accountRepository.observeAll().first()
                .map { it.currencyCode }
                .filter { it != defaultCurrency }
                .distinct()
            if (others.isEmpty()) {
                transientState.update { it.copy(ratesMessage = "No other currencies to refresh.") }
                return@launch
            }
            transientState.update { it.copy(refreshingRates = true, ratesMessage = null) }
            val result = exchangeRateRepository.refreshLatest(defaultCurrency, others)
            transientState.update {
                it.copy(
                    refreshingRates = false,
                    ratesMessage = if (result.isSuccess) "Rates updated." else "Refresh failed.",
                )
            }
        }
    }

    fun clearRatesMessage() {
        transientState.update { it.copy(ratesMessage = null) }
    }

    private data class TransientSettingsState(
        val refreshingRates: Boolean = false,
        val ratesMessage: String? = null,
    )

    private companion object {
        const val STATE_TIMEOUT_MILLIS = 5_000L
        const val MAX_CURRENCY_LENGTH = 3
    }
}
