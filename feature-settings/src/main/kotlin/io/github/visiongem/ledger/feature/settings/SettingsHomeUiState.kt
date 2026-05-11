package io.github.visiongem.ledger.feature.settings

import io.github.visiongem.ledger.core.data.domain.ThemeMode
import java.time.Instant

data class SettingsHomeUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultCurrency: String = "USD",
    val versionName: String = "0.1.0",
    val refreshingRates: Boolean = false,
    val ratesMessage: String? = null,
    val lastRateRefreshAt: Instant? = null,
    val backupBusy: Boolean = false,
    val backupMessage: String? = null,
)
