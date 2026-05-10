package io.github.visiongem.ledger.core.data.local.mapper

import io.github.visiongem.ledger.core.data.domain.Record
import io.github.visiongem.ledger.core.data.local.entity.RecordEntity

fun RecordEntity.toDomain(): Record = Record(
    id = id,
    accountId = accountId,
    categoryId = categoryId,
    type = type,
    amount = amount,
    occurredOn = occurredOn,
    note = note,
    transferToAccountId = transferToAccountId,
    transferAmount = transferAmount,
)

fun Record.toEntity(): RecordEntity = RecordEntity(
    id = id,
    accountId = accountId,
    categoryId = categoryId,
    type = type,
    amount = amount,
    occurredOn = occurredOn,
    note = note,
    transferToAccountId = transferToAccountId,
    transferAmount = transferAmount,
)
