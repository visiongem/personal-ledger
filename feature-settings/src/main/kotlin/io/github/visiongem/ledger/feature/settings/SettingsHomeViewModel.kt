package io.github.visiongem.ledger.feature.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.visiongem.ledger.core.base.BaseViewModel
import io.github.visiongem.ledger.core.data.backup.RecordBackupRepository
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
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val accountRepository: AccountRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val recordBackupRepository: RecordBackupRepository,
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
            backupBusy = transient.backupBusy,
            backupMessage = transient.backupMessage,
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
        // Defense in depth: drop non-letters, uppercase, keep up to 3 chars.
        // Then require exactly 3 letters — otherwise empty / 2-letter / digit-only input
        // would overwrite the previously-saved currency with garbage.
        val normalized = currency.filter { it.isLetter() }.uppercase().take(MAX_CURRENCY_LENGTH)
        if (normalized.length != MAX_CURRENCY_LENGTH) return
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
                transientState.update { it.copy(ratesMessage = context.getString(R.string.settings_rates_no_others)) }
                return@launch
            }
            transientState.update { it.copy(refreshingRates = true, ratesMessage = null) }
            val result = exchangeRateRepository.refreshLatest(defaultCurrency, others)
            transientState.update {
                it.copy(
                    refreshingRates = false,
                    ratesMessage = context.getString(
                        if (result.isSuccess) R.string.settings_rates_updated
                        else R.string.settings_rates_failed
                    ),
                )
            }
        }
    }

    fun exportRecordsToUri(uri: Uri) {
        viewModelScope.launch {
            transientState.update { it.copy(backupBusy = true, backupMessage = null) }
            val csv = recordBackupRepository.exportToCsv()
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(csv.toByteArray(Charsets.UTF_8))
                } ?: error("Couldn't open destination for writing")
            }.isSuccess
            transientState.update {
                it.copy(
                    backupBusy = false,
                    backupMessage = context.getString(
                        if (ok) R.string.settings_backup_exported
                        else R.string.settings_backup_export_failed
                    ),
                )
            }
        }
    }

    fun importRecordsFromUri(uri: Uri) {
        viewModelScope.launch {
            transientState.update { it.copy(backupBusy = true, backupMessage = null) }
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }
            }.getOrNull()
            if (text == null) {
                transientState.update {
                    it.copy(
                        backupBusy = false,
                        backupMessage = context.getString(R.string.settings_backup_read_failed),
                    )
                }
                return@launch
            }
            val result = recordBackupRepository.importFromCsv(text)
            transientState.update {
                it.copy(
                    backupBusy = false,
                    backupMessage = result.fold(
                        onSuccess = { count ->
                            context.getString(R.string.settings_backup_imported_fmt, count)
                        },
                        onFailure = { e ->
                            val reason = e.message ?: context.getString(R.string.settings_backup_unknown_error)
                            context.getString(R.string.settings_backup_import_failed_fmt, reason)
                        },
                    ),
                )
            }
        }
    }

    private data class TransientSettingsState(
        val refreshingRates: Boolean = false,
        val ratesMessage: String? = null,
        val backupBusy: Boolean = false,
        val backupMessage: String? = null,
    )

    private companion object {
        const val STATE_TIMEOUT_MILLIS = 5_000L
        const val MAX_CURRENCY_LENGTH = 3
    }
}
