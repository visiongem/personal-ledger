package io.github.visiongem.ledger.feature.account.list

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.visiongem.ledger.core.base.BaseViewModel
import io.github.visiongem.ledger.core.data.domain.Account
import io.github.visiongem.ledger.core.data.repo.AccountRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AccountListViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
) : BaseViewModel() {

    val state: StateFlow<AccountListUiState> = accountRepository.observeActive()
        .map { accounts -> AccountListUiState(accounts = accounts, loading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STATE_TIMEOUT_MILLIS),
            initialValue = AccountListUiState(),
        )

    private val _archiveEvents = MutableSharedFlow<Account>(extraBufferCapacity = 1)
    val archiveEvents: SharedFlow<Account> = _archiveEvents.asSharedFlow()

    fun onLongPress(account: Account) {
        viewModelScope.launch {
            accountRepository.upsert(account.copy(archived = true))
            _archiveEvents.tryEmit(account)
        }
    }

    fun undoArchive(account: Account) {
        viewModelScope.launch {
            accountRepository.upsert(account.copy(archived = false))
        }
    }

    private companion object {
        const val STATE_TIMEOUT_MILLIS = 5_000L
    }
}
