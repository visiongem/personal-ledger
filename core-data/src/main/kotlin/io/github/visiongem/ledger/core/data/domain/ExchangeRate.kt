package io.github.visiongem.ledger.core.data.domain

import java.math.BigDecimal
import java.time.LocalDate

data class ExchangeRate(
    val baseCurrency: String,
    val quoteCurrency: String,
    val rate: BigDecimal,
    val asOf: LocalDate,
)
