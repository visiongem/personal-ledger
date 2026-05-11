package io.github.visiongem.ledger.feature.settings

import io.github.visiongem.ledger.core.data.domain.ThemeMode

data class SettingsHomeUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultCurrency: String = "USD",
    val versionName: String = "0.1.0",
    val refreshingRates: Boolean = false,
    val ratesMessage: String? = null,
)
