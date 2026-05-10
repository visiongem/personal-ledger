package io.github.visiongem.ledger.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.Instant

@Entity(tableName = "account")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val currencyCode: String,
    val openingBalance: BigDecimal,
    val archived: Boolean,
    val createdAt: Instant,
)
