package io.github.visiongem.ledger.core.data.repo

import io.github.visiongem.ledger.core.data.domain.Record
import io.github.visiongem.ledger.core.data.domain.RecordType
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface RecordRepository {
    fun observeById(id: Long): Flow<Record?>
    fun observeByAccount(accountId: Long): Flow<List<Record>>
    fun observeInRange(start: LocalDate, end: LocalDate): Flow<List<Record>>
    fun observeByCategoryInRange(
        categoryId: Long,
        start: LocalDate,
        end: LocalDate,
    ): Flow<List<Record>>

    fun observeCategoryTotalsInRange(
        type: RecordType,
        start: LocalDate,
        end: LocalDate,
    ): Flow<Map<Long, BigDecimal>>

    suspend fun upsert(record: Record): Long
    suspend fun deleteById(id: Long)
}
