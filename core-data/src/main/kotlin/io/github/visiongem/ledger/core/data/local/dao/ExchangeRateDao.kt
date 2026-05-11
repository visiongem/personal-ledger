package io.github.visiongem.ledger.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.github.visiongem.ledger.core.data.local.entity.ExchangeRateEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface ExchangeRateDao {

    @Upsert
    suspend fun upsertAll(rates: List<ExchangeRateEntity>)

    @Query(
        """
        SELECT * FROM exchange_rate
        WHERE baseCurrency = :base AND quoteCurrency = :quote AND asOf = :asOf
        """
    )
    suspend fun getRate(base: String, quote: String, asOf: LocalDate): ExchangeRateEntity?

    @Query(
        """
        SELECT * FROM exchange_rate
        WHERE baseCurrency = :base AND quoteCurrency = :quote
        ORDER BY asOf DESC
        LIMIT 1
        """
    )
    fun observeLatest(base: String, quote: String): Flow<ExchangeRateEntity?>

    @Query("DELETE FROM exchange_rate WHERE asOf < :before")
    suspend fun deleteBefore(before: LocalDate)

    @Query("SELECT * FROM exchange_rate")
    suspend fun getAll(): List<ExchangeRateEntity>
}
