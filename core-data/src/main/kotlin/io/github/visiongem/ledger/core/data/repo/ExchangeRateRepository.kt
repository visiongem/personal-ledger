package io.github.visiongem.ledger.core.data.repo

import io.github.visiongem.ledger.core.data.domain.ExchangeRate
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface ExchangeRateRepository {
    fun observeLatest(base: String, quote: String): Flow<ExchangeRate?>
    suspend fun getRateOnDate(base: String, quote: String, asOf: LocalDate): ExchangeRate?
    suspend fun refreshLatest(base: String, quotes: List<String>): Result<List<ExchangeRate>>
}
