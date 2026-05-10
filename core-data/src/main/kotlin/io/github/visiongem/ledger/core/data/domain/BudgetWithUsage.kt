package io.github.visiongem.ledger.core.data.domain

import java.math.BigDecimal

data class BudgetWithUsage(
    val budget: Budget,
    val usage: BigDecimal,
)
