package io.github.visiongem.ledger.core.data.domain

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultCurrency: String = "USD",
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }
