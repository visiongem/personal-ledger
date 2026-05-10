package io.github.visiongem.ledger.core.data.local.mapper

import com.google.common.truth.Truth.assertThat
import io.github.visiongem.ledger.core.data.domain.Budget
import io.github.visiongem.ledger.core.data.local.entity.BudgetEntity
import java.math.BigDecimal
import java.time.YearMonth
import org.junit.jupiter.api.Test

class BudgetMapperTest {

    private val sampleEntity = BudgetEntity(
        id = 9L,
        categoryId = 5L,
        month = YearMonth.of(2026, 5),
        limit = BigDecimal("1500.00"),
        currencyCode = "USD",
    )

    private val sampleDomain = Budget(
        id = 9L,
        categoryId = 5L,
        month = YearMonth.of(2026, 5),
        limit = BigDecimal("1500.00"),
        currencyCode = "USD",
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
}
