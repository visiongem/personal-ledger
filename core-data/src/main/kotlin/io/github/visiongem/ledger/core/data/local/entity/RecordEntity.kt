package io.github.visiongem.ledger.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.visiongem.ledger.core.data.domain.RecordType
import java.math.BigDecimal
import java.time.LocalDate

@Entity(
    tableName = "record",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["transferToAccountId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["categoryId"]),
        Index(value = ["occurredOn"]),
        Index(value = ["transferToAccountId"]),
    ],
)
data class RecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val accountId: Long,
    val categoryId: Long?,
    val type: RecordType,
    val amount: BigDecimal,
    val occurredOn: LocalDate,
    val note: String?,
    val transferToAccountId: Long?,
    val transferAmount: BigDecimal?,
)
