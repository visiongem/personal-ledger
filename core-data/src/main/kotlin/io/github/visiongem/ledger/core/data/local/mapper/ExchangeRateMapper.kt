package io.github.visiongem.ledger.core.data.local.mapper

import io.github.visiongem.ledger.core.data.domain.ExchangeRate
import io.github.visiongem.ledger.core.data.local.entity.ExchangeRateEntity

fun ExchangeRateEntity.toDomain(): ExchangeRate = ExchangeRate(
    baseCurrency = baseCurrency,
    quoteCurrency = quoteCurrency,
    rate = rate,
    asOf = asOf,
)

fun ExchangeRate.toEntity(): ExchangeRateEntity = ExchangeRateEntity(
    baseCurrency = baseCurrency,
    quoteCurrency = quoteCurrency,
    rate = rate,
    asOf = asOf,
)
