package io.github.visiongem.ledger.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import java.math.BigDecimal
import java.time.LocalDate

@Entity(
    tableName = "exchange_rate",
    primaryKeys = ["baseCurrency", "quoteCurrency", "asOf"],
    indices = [Index(value = ["asOf"])],
)
data class ExchangeRateEntity(
    val baseCurrency: String,
    val quoteCurrency: String,
    val rate: BigDecimal,
    val asOf: LocalDate,
)
