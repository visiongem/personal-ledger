package io.github.visiongem.ledger.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.github.visiongem.ledger.core.data.local.entity.BudgetEntity
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Upsert
    suspend fun upsert(budget: BudgetEntity): Long

    @Query("DELETE FROM budget WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM budget WHERE id = :id")
    fun observeById(id: Long): Flow<BudgetEntity?>

    @Query("SELECT * FROM budget WHERE month = :month ORDER BY categoryId ASC")
    fun observeByMonth(month: YearMonth): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budget WHERE categoryId = :categoryId AND month = :month")
    fun observeByCategoryAndMonth(categoryId: Long, month: YearMonth): Flow<BudgetEntity?>
}
