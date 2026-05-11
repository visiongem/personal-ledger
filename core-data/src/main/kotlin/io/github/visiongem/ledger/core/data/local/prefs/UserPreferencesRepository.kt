package io.github.visiongem.ledger.core.data.local.prefs

import io.github.visiongem.ledger.core.data.domain.ThemeMode
import io.github.visiongem.ledger.core.data.domain.UserPreferences
import java.time.Instant
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val flow: Flow<UserPreferences>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDefaultCurrency(currency: String)
    suspend fun setLastRateRefreshAt(instant: Instant)
}
