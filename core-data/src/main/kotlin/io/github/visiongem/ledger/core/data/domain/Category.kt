package io.github.visiongem.ledger.core.data.domain

data class Category(
    val id: Long = 0L,
    val name: String,
    val type: CategoryType,
    val iconKey: String? = null,
    val sortOrder: Int = 0,
)

enum class CategoryType { INCOME, EXPENSE }
