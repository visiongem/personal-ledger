package io.github.visiongem.ledger.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.visiongem.ledger.core.data.domain.CategoryType

@Entity(tableName = "category")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val type: CategoryType,
    val iconKey: String?,
    val sortOrder: Int,
)
