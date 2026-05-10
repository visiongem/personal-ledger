package io.github.visiongem.ledger.core.data.local.mapper

import io.github.visiongem.ledger.core.data.domain.Category
import io.github.visiongem.ledger.core.data.local.entity.CategoryEntity

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    type = type,
    iconKey = iconKey,
    sortOrder = sortOrder,
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    type = type,
    iconKey = iconKey,
    sortOrder = sortOrder,
)
