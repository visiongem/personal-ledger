package io.github.visiongem.ledger.feature.account.edit

data class AccountEditUiState(
    val id: Long? = null,
    val name: String = "",
    val currencyCode: String = "USD",
    val openingBalance: String = "0",
    val saving: Boolean = false,
    val saved: Boolean = false,
    val errorMessage: String? = null,
)
