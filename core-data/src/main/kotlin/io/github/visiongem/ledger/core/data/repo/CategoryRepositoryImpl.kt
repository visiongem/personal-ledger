package io.github.visiongem.ledger.core.data.repo

import io.github.visiongem.ledger.core.data.domain.Category
import io.github.visiongem.ledger.core.data.domain.CategoryType
import io.github.visiongem.ledger.core.data.local.dao.CategoryDao
import io.github.visiongem.ledger.core.data.local.mapper.toDomain
import io.github.visiongem.ledger.core.data.local.mapper.toEntity
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepositoryImpl @Inject constructor(
    private val dao: CategoryDao,
) : CategoryRepository {

    override fun observeAll(): Flow<List<Category>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeByType(type: CategoryType): Flow<List<Category>> =
        dao.observeByType(type).map { list -> list.map { it.toDomain() } }

    override fun observeById(id: Long): Flow<Category?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun upsert(category: Category): Long =
        dao.upsert(category.toEntity())

    override suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }
}
