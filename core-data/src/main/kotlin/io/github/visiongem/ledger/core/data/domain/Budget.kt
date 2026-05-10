package io.github.visiongem.ledger.core.data.domain

import java.math.BigDecimal
import java.time.YearMonth

data class Budget(
    val id: Long = 0L,
    val categoryId: Long,
    val month: YearMonth,
    val limit: BigDecimal,
    val currencyCode: String,
)
