package io.github.visiongem.ledger.core.data.local.mapper

import io.github.visiongem.ledger.core.data.domain.Account
import io.github.visiongem.ledger.core.data.local.entity.AccountEntity

fun AccountEntity.toDomain(): Account = Account(
    id = id,
    name = name,
    currencyCode = currencyCode,
    openingBalance = openingBalance,
    archived = archived,
    createdAt = createdAt,
)

fun Account.toEntity(): AccountEntity = AccountEntity(
    id = id,
    name = name,
    currencyCode = currencyCode,
    openingBalance = openingBalance,
    archived = archived,
    createdAt = createdAt,
)
