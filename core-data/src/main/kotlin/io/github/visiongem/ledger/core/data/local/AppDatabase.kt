package io.github.visiongem.ledger.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.visiongem.ledger.core.data.local.converter.LedgerTypeConverters
import io.github.visiongem.ledger.core.data.local.dao.AccountDao
import io.github.visiongem.ledger.core.data.local.dao.BudgetDao
import io.github.visiongem.ledger.core.data.local.dao.CategoryDao
import io.github.visiongem.ledger.core.data.local.dao.ExchangeRateDao
import io.github.visiongem.ledger.core.data.local.dao.RecordDao
import io.github.visiongem.ledger.core.data.local.entity.AccountEntity
import io.github.visiongem.ledger.core.data.local.entity.BudgetEntity
import io.github.visiongem.ledger.core.data.local.entity.CategoryEntity
import io.github.visiongem.ledger.core.data.local.entity.ExchangeRateEntity
import io.github.visiongem.ledger.core.data.local.entity.RecordEntity

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        RecordEntity::class,
        BudgetEntity::class,
        ExchangeRateEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(LedgerTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun recordDao(): RecordDao
    abstract fun budgetDao(): BudgetDao
    abstract fun exchangeRateDao(): ExchangeRateDao

    companion object {
        const val DATABASE_NAME = "ledger.db"
    }
}
