package io.github.visiongem.ledger.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.github.visiongem.ledger.core.data.domain.CategoryType
import io.github.visiongem.ledger.core.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Upsert
    suspend fun upsert(category: CategoryEntity): Long

    @Query("DELETE FROM category WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM category ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM category WHERE type = :type ORDER BY sortOrder ASC")
    fun observeByType(type: CategoryType): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM category WHERE id = :id")
    fun observeById(id: Long): Flow<CategoryEntity?>
}
