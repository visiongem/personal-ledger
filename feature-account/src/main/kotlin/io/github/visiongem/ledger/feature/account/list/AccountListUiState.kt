package io.github.visiongem.ledger.feature.account.list

import io.github.visiongem.ledger.core.data.domain.Account

data class AccountListUiState(
    val accounts: List<Account> = emptyList(),
    val loading: Boolean = true,
)
