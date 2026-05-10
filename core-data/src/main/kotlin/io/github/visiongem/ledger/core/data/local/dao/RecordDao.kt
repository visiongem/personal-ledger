package io.github.visiongem.ledger.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.github.visiongem.ledger.core.data.local.entity.RecordEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

// Amount is persisted as String via TypeConverter, so SQL aggregates (SUM/AVG)
// are not used here. Repositories sum BigDecimal in Kotlin after fetching.
@Dao
interface RecordDao {

    @Upsert
    suspend fun upsert(record: RecordEntity): Long

    @Query("DELETE FROM record WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM record WHERE id = :id")
    fun observeById(id: Long): Flow<RecordEntity?>

    @Query("SELECT * FROM record WHERE accountId = :accountId ORDER BY occurredOn DESC, id DESC")
    fun observeByAccount(accountId: Long): Flow<List<RecordEntity>>

    @Query(
        """
        SELECT * FROM record
        WHERE occurredOn BETWEEN :startInclusive AND :endInclusive
        ORDER BY occurredOn DESC, id DESC
        """
    )
    fun observeInRange(startInclusive: LocalDate, endInclusive: LocalDate): Flow<List<RecordEntity>>

    @Query(
        """
        SELECT * FROM record
        WHERE categoryId = :categoryId
          AND occurredOn BETWEEN :startInclusive AND :endInclusive
        ORDER BY occurredOn DESC, id DESC
        """
    )
    fun observeByCategoryInRange(
        categoryId: Long,
        startInclusive: LocalDate,
        endInclusive: LocalDate,
    ): Flow<List<RecordEntity>>
}
