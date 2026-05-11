package io.github.visiongem.ledger.feature.account.edit

import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.visiongem.ledger.core.base.BaseViewModel
import io.github.visiongem.ledger.core.data.domain.Account
import io.github.visiongem.ledger.core.data.repo.AccountRepository
import io.github.visiongem.ledger.feature.account.R
import java.math.BigDecimal
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

@HiltViewModel
class AccountEditViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accountRepository: AccountRepository,
) : BaseViewModel() {

    private val _state = MutableStateFlow(AccountEditUiState())
    val state: StateFlow<AccountEditUiState> = _state.asStateFlow()

    fun loadIfNeeded(id: Long?) {
        if (id == null) return
        if (_state.value.id == id) return
        launchCatching {
            val account = accountRepository.observeById(id).first() ?: return@launchCatching
            _state.update {
                it.copy(
                    id = account.id,
                    name = account.name,
                    currencyCode = account.currencyCode,
                    openingBalance = account.openingBalance.toPlainString(),
                )
            }
        }
    }

    fun onNameChange(value: String) {
        _state.update { it.copy(name = value, errorMessage = null) }
    }

    fun onCurrencyChange(value: String) {
        _state.update { it.copy(currencyCode = value.uppercase(), errorMessage = null) }
    }

    fun onOpeningBalanceChange(value: String) {
        _state.update { it.copy(openingBalance = value, errorMessage = null) }
    }

    fun save() {
        val current = _state.value
        val trimmedName = current.name.trim()
        if (trimmedName.isEmpty()) {
            _state.update { it.copy(errorMessage = context.getString(R.string.account_err_name_required)) }
            return
        }
        if (current.currencyCode.length != ISO_CURRENCY_LENGTH) {
            _state.update { it.copy(errorMessage = context.getString(R.string.account_err_currency_invalid)) }
            return
        }
        val balance = runCatching {
            BigDecimal(current.openingBalance.ifBlank { "0" })
        }.getOrNull()
        if (balance == null) {
            _state.update { it.copy(errorMessage = context.getString(R.string.account_err_balance_invalid)) }
            return
        }

        launchCatching(
            onError = { error ->
                _state.update {
                    it.copy(
                        saving = false,
                        errorMessage = error.message ?: context.getString(R.string.account_err_save_failed),
                    )
                }
            }
        ) {
            _state.update { it.copy(saving = true, errorMessage = null) }
            accountRepository.upsert(
                Account(
                    id = current.id ?: 0L,
                    name = trimmedName,
                    currencyCode = current.currencyCode,
                    openingBalance = balance,
                    archived = false,
                    createdAt = Instant.now(),
                )
            )
            _state.update { it.copy(saving = false, saved = true) }
        }
    }

    private companion object {
        const val ISO_CURRENCY_LENGTH = 3
    }
}
