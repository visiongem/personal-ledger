package io.github.visiongem.ledger.core.data.repo

import io.github.visiongem.ledger.core.data.domain.Category
import io.github.visiongem.ledger.core.data.domain.CategoryType
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeAll(): Flow<List<Category>>
    fun observeByType(type: CategoryType): Flow<List<Category>>
    fun observeById(id: Long): Flow<Category?>
    suspend fun upsert(category: Category): Long
    suspend fun deleteById(id: Long)
}
