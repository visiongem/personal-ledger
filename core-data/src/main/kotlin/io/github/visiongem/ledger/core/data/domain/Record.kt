package io.github.visiongem.ledger.core.data.domain

import java.math.BigDecimal
import java.time.LocalDate

data class Record(
    val id: Long = 0L,
    val accountId: Long,
    val categoryId: Long?,
    val type: RecordType,
    val amount: BigDecimal,
    val occurredOn: LocalDate,
    val note: String? = null,
    val transferToAccountId: Long? = null,
    val transferAmount: BigDecimal? = null,
)

enum class RecordType { INCOME, EXPENSE, TRANSFER }
