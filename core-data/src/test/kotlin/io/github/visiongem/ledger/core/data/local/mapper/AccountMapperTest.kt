package io.github.visiongem.ledger.core.data.local.mapper

import com.google.common.truth.Truth.assertThat
import io.github.visiongem.ledger.core.data.domain.Account
import io.github.visiongem.ledger.core.data.local.entity.AccountEntity
import java.math.BigDecimal
import java.time.Instant
import org.junit.jupiter.api.Test

class AccountMapperTest {

    private val sampleEntity = AccountEntity(
        id = 7L,
        name = "Cash",
        currencyCode = "USD",
        openingBalance = BigDecimal("100.50"),
        archived = false,
        createdAt = Instant.parse("2026-05-10T10:00:00Z"),
    )

    private val sampleDomain = Account(
        id = 7L,
        name = "Cash",
        currencyCode = "USD",
        openingBalance = BigDecimal("100.50"),
        archived = false,
        createdAt = Instant.parse("2026-05-10T10:00:00Z"),
    )

    @Test fun entityToDomainPreservesAllFields() {
        val domain = sampleEntity.toDomain()
        assertThat(domain).isEqualTo(sampleDomain)
    }

    @Test fun domainToEntityPreservesAllFields() {
        val entity = sampleDomain.toEntity()
        assertThat(entity).isEqualTo(sampleEntity)
    }

    @Test fun roundTripFromEntity() {
        assertThat(sampleEntity.toDomain().toEntity()).isEqualTo(sampleEntity)
    }

    @Test fun roundTripFromDomain() {
        assertThat(sampleDomain.toEntity().toDomain()).isEqualTo(sampleDomain)
    }
}
