package io.github.visiongem.ledger.core.data.local.mapper

import io.github.visiongem.ledger.core.data.domain.Budget
import io.github.visiongem.ledger.core.data.local.entity.BudgetEntity

fun BudgetEntity.toDomain(): Budget = Budget(
    id = id,
    categoryId = categoryId,
    month = month,
    limit = limit,
    currencyCode = currencyCode,
)

fun Budget.toEntity(): BudgetEntity = BudgetEntity(
    id = id,
    categoryId = categoryId,
    month = month,
    limit = limit,
    currencyCode = currencyCode,
)
