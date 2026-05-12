package io.github.visiongem.ledger.feature.settings.category

import io.github.visiongem.ledger.core.data.domain.CategoryType

data class CategoryEditUiState(
    val id: Long? = null,
    val name: String = "",
    val type: CategoryType = CategoryType.EXPENSE,
    val isEditing: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val errorMessage: String? = null,
)
