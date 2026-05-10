package io.github.visiongem.ledger.feature.settings

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.visiongem.ledger.core.base.BaseViewModel
import io.github.visiongem.ledger.core.data.domain.ThemeMode
import io.github.visiongem.ledger.core.data.local.prefs.UserPreferencesRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsHomeViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) : BaseViewModel() {

    val state: StateFlow<SettingsHomeUiState> = userPreferencesRepository.flow
        .map { prefs ->
            SettingsHomeUiState(
                themeMode = prefs.themeMode,
                defaultCurrency = prefs.defaultCurrency,
            )
        }
        .stateIn(
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

    private companion object {
        const val STATE_TIMEOUT_MILLIS = 5_000L
        const val MAX_CURRENCY_LENGTH = 3
    }
}
