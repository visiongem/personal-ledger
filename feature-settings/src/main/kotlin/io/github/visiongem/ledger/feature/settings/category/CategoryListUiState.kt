package io.github.visiongem.ledger.feature.settings.category

import io.github.visiongem.ledger.core.data.domain.Category

data class CategoryListUiState(
    val expense: List<Category> = emptyList(),
    val income: List<Category> = emptyList(),
    val loading: Boolean = true,
)
