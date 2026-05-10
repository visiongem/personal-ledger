package io.github.visiongem.ledger.core.data.repo

import io.github.visiongem.ledger.core.data.domain.ExchangeRate
import io.github.visiongem.ledger.core.data.local.dao.ExchangeRateDao
import io.github.visiongem.ledger.core.data.local.mapper.toDomain
import io.github.visiongem.ledger.core.data.local.mapper.toEntity
import io.github.visiongem.ledger.core.data.remote.FrankfurterApi
import io.github.visiongem.ledger.core.data.remote.FrankfurterRatesResponse
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ExchangeRateRepositoryImpl @Inject constructor(
    private val dao: ExchangeRateDao,
    private val api: FrankfurterApi,
) : ExchangeRateRepository {

    override fun observeLatest(base: String, quote: String): Flow<ExchangeRate?> =
        dao.observeLatest(base, quote).map { it?.toDomain() }

    override suspend fun getRateOnDate(
        base: String,
        quote: String,
        asOf: LocalDate,
    ): ExchangeRate? {
        dao.getRate(base, quote, asOf)?.let { return it.toDomain() }

        val response = runCatching {
            api.getOnDate(date = asOf.toString(), base = base, symbols = quote)
        }.getOrElse { return null }

        val rate = response.toRates(asOf).firstOrNull { it.quoteCurrency == quote } ?: return null
        dao.upsertAll(listOf(rate.toEntity()))
        return rate
    }

    override suspend fun refreshLatest(
        base: String,
        quotes: List<String>,
    ): Result<List<ExchangeRate>> = runCatching {
        val response = api.getLatest(base = base, symbols = quotes.joinToString(","))
        val asOf = LocalDate.parse(response.date)
        val rates = response.toRates(asOf)
        dao.upsertAll(rates.map { it.toEntity() })
        rates
    }

    private fun FrankfurterRatesResponse.toRates(asOf: LocalDate): List<ExchangeRate> =
        rates.map { (quote, value) ->
            ExchangeRate(
                baseCurrency = base,
                quoteCurrency = quote,
                rate = BigDecimal.valueOf(value),
                asOf = asOf,
            )
        }
}
