package io.github.visiongem.ledger.core.data.local.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.visiongem.ledger.core.data.domain.ThemeMode
import io.github.visiongem.ledger.core.data.domain.UserPreferences
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : UserPreferencesRepository {

    override val flow: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            themeMode = prefs[KEY_THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            defaultCurrency = prefs[KEY_DEFAULT_CURRENCY] ?: DEFAULT_CURRENCY,
        )
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    override suspend fun setDefaultCurrency(currency: String) {
        dataStore.edit { it[KEY_DEFAULT_CURRENCY] = currency }
    }

    private companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_DEFAULT_CURRENCY = stringPreferencesKey("default_currency")
        const val DEFAULT_CURRENCY = "USD"
    }
}
