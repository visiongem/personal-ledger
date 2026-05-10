package io.github.visiongem.ledger.feature.account.list

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.visiongem.ledger.core.base.BaseViewModel
import io.github.visiongem.ledger.core.data.repo.AccountRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class AccountListViewModel @Inject constructor(
    accountRepository: AccountRepository,
) : BaseViewModel() {

    val state: StateFlow<AccountListUiState> = accountRepository.observeActive()
        .map { accounts -> AccountListUiState(accounts = accounts, loading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STATE_TIMEOUT_MILLIS),
            initialValue = AccountListUiState(),
        )

    private companion object {
        const val STATE_TIMEOUT_MILLIS = 5_000L
    }
}
