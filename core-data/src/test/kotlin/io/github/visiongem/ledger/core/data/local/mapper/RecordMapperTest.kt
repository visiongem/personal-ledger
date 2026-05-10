package io.github.visiongem.ledger.core.data.local.mapper

import com.google.common.truth.Truth.assertThat
import io.github.visiongem.ledger.core.data.domain.Record
import io.github.visiongem.ledger.core.data.domain.RecordType
import io.github.visiongem.ledger.core.data.local.entity.RecordEntity
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.jupiter.api.Test

class RecordMapperTest {

    private val expenseEntity = RecordEntity(
        id = 1L,
        accountId = 10L,
        categoryId = 20L,
        type = RecordType.EXPENSE,
        amount = BigDecimal("12.34"),
        occurredOn = LocalDate.of(2026, 5, 10),
        note = "Coffee",
        transferToAccountId = null,
        transferAmount = null,
    )

    private val transferEntity = expenseEntity.copy(
        type = RecordType.TRANSFER,
        categoryId = null,
        transferToAccountId = 11L,
        transferAmount = BigDecimal("88.00"),
        note = null,
    )

    @Test fun expenseEntityToDomainPreservesFields() {
        val domain = expenseEntity.toDomain()
        assertThat(domain.type).isEqualTo(RecordType.EXPENSE)
        assertThat(domain.amount).isEqualTo(BigDecimal("12.34"))
        assertThat(domain.note).isEqualTo("Coffee")
        assertThat(domain.transferToAccountId).isNull()
    }

    @Test fun transferEntityToDomainPreservesFields() {
        val domain = transferEntity.toDomain()
        assertThat(domain.type).isEqualTo(RecordType.TRANSFER)
        assertThat(domain.categoryId).isNull()
        assertThat(domain.transferToAccountId).isEqualTo(11L)
        assertThat(domain.transferAmount).isEqualTo(BigDecimal("88.00"))
    }

    @Test fun expenseRoundTrip() {
        assertThat(expenseEntity.toDomain().toEntity()).isEqualTo(expenseEntity)
    }

    @Test fun transferRoundTrip() {
        assertThat(transferEntity.toDomain().toEntity()).isEqualTo(transferEntity)
    }

    @Test fun domainToEntityPreservesAllFields() {
        val domain = Record(
            id = 5L,
            accountId = 10L,
            categoryId = 30L,
            type = RecordType.INCOME,
            amount = BigDecimal("500.00"),
            occurredOn = LocalDate.of(2026, 5, 1),
            note = "Salary",
        )
        val entity = domain.toEntity()
        assertThat(entity.id).isEqualTo(5L)
        assertThat(entity.type).isEqualTo(RecordType.INCOME)
        assertThat(entity.amount).isEqualTo(BigDecimal("500.00"))
    }
}
