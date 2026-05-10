package io.github.visiongem.ledger.core.data.domain

import java.math.BigDecimal
import java.time.Instant

data class Account(
    val id: Long = 0L,
    val name: String,
    val currencyCode: String,
    val openingBalance: BigDecimal = BigDecimal.ZERO,
    val archived: Boolean = false,
    val createdAt: Instant,
)
