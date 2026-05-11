package io.github.visiongem.ledger.core.data.domain

import java.time.Instant

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultCurrency: String = "USD",
    val lastRateRefreshAt: Instant? = null,
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }
