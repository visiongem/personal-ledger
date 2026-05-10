package io.github.visiongem.ledger.core.data.local.mapper

import com.google.common.truth.Truth.assertThat
import io.github.visiongem.ledger.core.data.domain.ExchangeRate
import io.github.visiongem.ledger.core.data.local.entity.ExchangeRateEntity
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.jupiter.api.Test

class ExchangeRateMapperTest {

    private val sampleEntity = ExchangeRateEntity(
        baseCurrency = "USD",
        quoteCurrency = "CNY",
        rate = BigDecimal("7.2345"),
        asOf = LocalDate.of(2026, 5, 10),
    )

    private val sampleDomain = ExchangeRate(
        baseCurrency = "USD",
        quoteCurrency = "CNY",
        rate = BigDecimal("7.2345"),
        asOf = LocalDate.of(2026, 5, 10),
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
