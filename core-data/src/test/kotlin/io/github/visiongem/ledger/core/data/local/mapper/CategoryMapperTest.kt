package io.github.visiongem.ledger.core.data.local.mapper

import com.google.common.truth.Truth.assertThat
import io.github.visiongem.ledger.core.data.domain.Category
import io.github.visiongem.ledger.core.data.domain.CategoryType
import io.github.visiongem.ledger.core.data.local.entity.CategoryEntity
import org.junit.jupiter.api.Test

class CategoryMapperTest {

    private val sampleEntity = CategoryEntity(
        id = 3L,
        name = "Food",
        type = CategoryType.EXPENSE,
        iconKey = "food",
        sortOrder = 1,
    )

    private val sampleDomain = Category(
        id = 3L,
        name = "Food",
        type = CategoryType.EXPENSE,
        iconKey = "food",
        sortOrder = 1,
    )

    @Test fun entityToDomainPreservesAllFields() {
        assertThat(sampleEntity.toDomain()).isEqualTo(sampleDomain)
    }

    @Test fun domainToEntityPreservesAllFields() {
        assertThat(sampleDomain.toEntity()).isEqualTo(sampleEntity)
    }

    @Test fun roundTripFromEntity() {
        assertThat(sampleEntity.toDomain().toEntity()).isEqualTo(sampleEntity)
    }

    @Test fun nullIconKeyHandled() {
        val noIconEntity = sampleEntity.copy(iconKey = null)
        assertThat(noIconEntity.toDomain().iconKey).isNull()
    }
}
